package au.org.ala.vocabulary

import au.org.ala.util.ResourceUtils
import au.org.ala.util.TitleCapitaliser
import com.opencsv.CSVParserBuilder
import com.opencsv.CSVReaderBuilder
import groovy.json.JsonSlurper
import org.apache.commons.validator.Form
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.model.util.ModelBuilder
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.OWL
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.SKOS
import org.eclipse.rdf4j.model.vocabulary.XMLSchema
import org.springframework.web.multipart.MultipartFile

class TranslationService {
    def repositoryService
    def messageSource

    static TYPE_639_MAP = [
            I: LanguageType.INDIVIDUAL,
            M: LanguageType.MACRO_LANGUAGE,
            S: LanguageType.SPECIAL,
            A: LanguageType.ANCIENT,
            C: LanguageType.CONSTRUCTED,
            E: LanguageType.EXTINCT,
            H: LanguageType.HISTORICAL,
            L: LanguageType.LIVING,
            U: LanguageType.UNKNOWN
    ]

    /**
     * Process ISO-639-3 name and macro-translation file into RDF
     *
     * @param nameFile The name file
     * @param macroFile The macro-translation file
     * @param tagLanguages The list of required translation codes
     * @param locale The request locale
     * @param complete Include all languages
     *
     * @return An RDF model of the results
     */
    Model process639(MultipartFile nameFile, MultipartFile macroFile, String tagLanguages, Locale locale, boolean complete) {
        def valueFactory = repositoryService.valueFactory
        def defaultType = TYPE_639_MAP['U']
        def namespace = ALAVocabulary.NAMESPACE + "iso639"
        def source = valueFactory.createIRI('http://www.iso639-3.sil.org')
        def builder = new ModelBuilder()
        builder.setNamespace(ALAVocabulary.PREFIX, ALAVocabulary.NAMESPACE)
        builder.setNamespace(ALA.PREFIX, ALA.NAMESPACE)
        builder.setNamespace(Format.PREFIX, Format.NAMESPACE)
        builder.setNamespace(SKOS.PREFIX, SKOS.NAMESPACE)
        builder.setNamespace(RDF.PREFIX, RDF.NAMESPACE)
        builder.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
        builder.setNamespace(OWL.PREFIX, OWL.NAMESPACE)
        builder.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE)
        def vocabulary = valueFactory.createIRI(namespace)
        builder.subject(vocabulary)
        builder.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
        builder.add(RDFS.LABEL, messageSource.getMessage('page.admin.process639.iso639.label', null, locale))
        builder.add(RDFS.COMMENT, messageSource.getMessage('page.admin.process639.iso639.comment', null, locale))
        builder.add(DCTERMS.SOURCE, source)

        def languages = [:]
        String[] line

        // Read the translation codes
        def reader = new InputStreamReader(nameFile.inputStream, 'UTF-8')
        def parser = new CSVParserBuilder().withSeparator('\t' as char).withIgnoreLeadingWhiteSpace(true).build()
        def csv = new CSVReaderBuilder(reader).withSkipLines(1).withCSVParser(parser).build()
        while ((line = csv.readNext()) != null) {
            def code = line[0]
            def part2b = line[1]
            def part2t = line[2]
            def part1 = line[3]
            def scope = line[4]  // I(ndividual), M(acrolanguage), S(pecial)
            def type = line[5] // A(ncient), C(onstructed), E(xtinct), H(istorical), L(iving), S(pecial)
            def name = line[6]
            def comments = line[7]
            def types = [TYPE_639_MAP[scope] ?: defaultType, TYPE_639_MAP[type] ?: defaultType] as Set
            def alts = [part1, part2t, part2b].findAll({ it && it != code}) as Set
            def iri = valueFactory.createIRI('http://iso639-3.sil.org/code/', code)
            def tag = part1 && type == 'L'
            def language = [iri: iri, code: code, name: name, types: types, alts: alts, tag: tag]
            if (part1) {
                language.part1 = part1
                language.part1Iri = valueFactory.createIRI('http://www.infoterm.info/iso639-1/', part1)
            }
            if (comments)
                language.comments = comments
            languages[code] = language
        }

        // Read the macro languages
        reader = new InputStreamReader(macroFile.inputStream, 'UTF-8')
        csv = new CSVReaderBuilder(reader).withSkipLines(1).withCSVParser(parser).build()
        while ((line = csv.readNext()) != null) {
            def macro = line[0]
            def lang = line[1]
            def status = line[2] // A (active) or R (retired)
            if (status == 'A') {
                lang = languages[lang]
                macro = languages[macro]
                if (lang && macro) {
                    if (!lang.macro)
                        lang.macro = []
                    lang.macro << macro
                    macro.tag = macro.tag || lang.tag
                }
            }
        }
        // Read the list of taggable languages
        tagLanguages.split(',').each { code ->
            def lang = languages[code]
            if (lang)
                lang.tag = true
        }

        // Build the model
        languages.each { code, lang ->
            if (lang.tag || complete) {
                builder.subject(lang.iri)
                builder.add(RDF.TYPE, DCTERMS.ISO639_3)
                if (lang.tag)
                    builder.add(RDF.TYPE, Format.LANGUAGE)
                builder.add(SKOS.IN_SCHEME, vocabulary)
                builder.add(SKOS.NOTATION, lang.code)
                builder.add(RDFS.LABEL, lang.name)
                lang.alts.each { alt -> builder.add(SKOS.ALT_LABEL, alt)}
                if (lang.part1Iri)
                    builder.add(SKOS.EXACT_MATCH, lang.part1Iri)
                if (lang.comment)
                    builder.add(RDFS.COMMENT, lang.comment)
                lang.types.each { type -> builder.add(Format.LANGUAGE_TYPE, type) }
                lang.macro.each { macro -> builder.add(SKOS.BROADER, macro.iri) }
                builder.add(DCTERMS.SOURCE, source)

                if (lang.part1Iri && lang.part1) {
                    builder.subject(lang.part1Iri)
                    builder.add(RDF.TYPE, DCTERMS.RFC5646)
                    if (lang.tag)
                        builder.add(RDF.TYPE, Format.LANGUAGE)
                    builder.add(SKOS.IN_SCHEME, vocabulary)
                    builder.add(SKOS.NOTATION, lang.part1)
                    builder.add(RDFS.LABEL, lang.name)
                    builder.add(SKOS.EXACT_MATCH, lang.iri)
                    lang.types.each { type -> builder.add(Format.LANGUAGE_TYPE, type) }
                    builder.add(DCTERMS.SOURCE, source)

                }
            }
        }
        return builder.build()
    }

    /**
     * Process an DwC description
     *
     * @param dwcFile The file containing the Darwin code terms
     * @param biocacheIndexFields URL to a biocache instance that contains
     * @param locale The locale for text info
     * @param complete Include all terms
     *
     * @return An RDF model of the results
     */
    Model processDwC(MultipartFile dwcFile, URL biocacheIndexFields, Locale locale, boolean complete) {
        def valueFactory = repositoryService.valueFactory
        def dwcSource = valueFactory.createIRI('http://rs.tdwg.org/dwc/terms')
        def alaSource = valueFactory.createIRI(biocacheIndexFields.toExternalForm())
        def builder = new ModelBuilder()
        builder.setNamespace(ALAVocabulary.PREFIX, ALAVocabulary.NAMESPACE)
        builder.setNamespace(ALA.PREFIX, ALA.NAMESPACE)
        builder.setNamespace(Format.PREFIX, Format.NAMESPACE)
        builder.setNamespace(SKOS.PREFIX, SKOS.NAMESPACE)
        builder.setNamespace(RDF.PREFIX, RDF.NAMESPACE)
        builder.setNamespace(RDFS.PREFIX, RDFS.NAMESPACE)
        builder.setNamespace(OWL.PREFIX, OWL.NAMESPACE)
        builder.setNamespace(DCTERMS.PREFIX, DCTERMS.NAMESPACE)
        def dwcVocabulary = valueFactory.createIRI('http://rs.tdwg.org/dwc/terms')
        builder.subject(dwcVocabulary)
        builder.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
        builder.add(RDFS.LABEL, messageSource.getMessage('page.admin.processDwC.dwc.label', null, locale))
        builder.add(RDFS.COMMENT, messageSource.getMessage('page.admin.processDwC.dwc.comment', null, locale))
        builder.add(DCTERMS.SOURCE, dwcSource)
        def alaVocabulary = valueFactory.createIRI('http://www.ala.org.au/terms')
        builder.subject(alaVocabulary)
        builder.add(RDF.TYPE, SKOS.CONCEPT_SCHEME)
        builder.add(RDFS.LABEL, messageSource.getMessage('page.admin.processDwC.ala.label', null, locale))
        builder.add(RDFS.COMMENT, messageSource.getMessage('page.admin.processDwC.ala.comment', null, locale))
        builder.add(DCTERMS.SOURCE, alaSource)
        def terms = []
        def lookup = [:]
        String[] line
        // Read the term defintions
        def reader = new InputStreamReader(dwcFile.inputStream, 'UTF-8')
        def parser = new CSVParserBuilder().withSeparator(',' as char).withQuoteChar('"' as char).withIgnoreLeadingWhiteSpace(true).build()
        def csv = new CSVReaderBuilder(reader).withSkipLines(1).withCSVParser(parser).build()
        while ((line = csv.readNext()) != null) {
            def iri = valueFactory.createIRI(line[0]) // Dated IRI
            def label = line[1].replaceFirst('^dcterms:', '')
            def definition = line[2]
            def comments = line[3]
            def examples = line[4]  // separated by commas
            def organised_in = line[5]
            def issued = line[6]
            def status = line[7]
            def replaces = line[8]
            def rdf_type = valueFactory.createIRI(line[9])
            def term_iri = valueFactory.createIRI(line[10])
            def abcd_equivalent = line[11]
            def flags = line[12]
            def current = status == 'recommended'
            def term = [
                    iri: term_iri,
                    notation: term_iri.localName,
                    label: label,
                    definition: definition,
                    comments: comments,
                    examples: examples,
                    organiser: organised_in,
                    issued: issued,
                    status: status,
                    replaces: replaces ? (replaces.split('\\|') as List)?.collect( { valueFactory.createIRI(it) } ) : null,
                    type: rdf_type,
                    version: iri,
                    abcd: abcd_equivalent,
                    flags: flags,
                    current: current,
                    dwc: true
            ]
            terms << term
            if (current) {
                lookup[term.iri.stringValue()] = term
                lookup[term.notation] = term
            }
        }
        // Read the biocache definitions
        if (biocacheIndexFields) {
            def js = new JsonSlurper()
            def index = js.parse(biocacheIndexFields)
            def capitaliser = TitleCapitaliser.create(locale.language)
            index.each { field ->
                String name = field.name
                def description = field.description
                def info = field.info
                def downloadName = field.downloadName
                def classs = field.classs
                def dwcTerm = field.dwcTerm
                def jsonName = field.jsonName
                //def i18nValues = field.i18nValues
                //def downloadDescription = field.downloadDescription
                def label = capitaliser.capitalise(name.split('_+'))
                def term = null
                if (dwcTerm)
                    term = lookup[dwcTerm]
                if (!term)
                    term = lookup[info]
                if (!term) {
                    term = [
                            iri: valueFactory.createIRI(ALA.NAMESPACE, name),
                            notation: name,
                            label: label,
                            definition: description ?: info,
                            comments: description && description != info ? info : null,
                            examples: null,
                            organiser: classs,
                            issued: null,
                            status: 'indexed',
                            replaces: null,
                            type: RDF.PROPERTY,
                            version: null,
                            abcd: null,
                            flags: null,
                            current: true,
                            dwc: false
                    ]
                    terms << term
                }
                if (!term.csvName && downloadName)
                    term.csvName = downloadName
                if (!term.jsonName && jsonName)
                    term.jsonName = jsonName
                if (!term.solrName)
                    term.solrName = name
            }
        }

        // Construct a model for the definitions
        terms.each { term ->
           if (complete || term.current) {
                builder.subject(term.iri)
                builder.add(RDF.TYPE, term.type)
                if (term.current)
                    builder.add(RDF.TYPE, Format.TERM)
                builder.add(SKOS.IN_SCHEME, alaVocabulary)
                if (term.dwc)
                    builder.add(SKOS.IN_SCHEME, dwcVocabulary)
                builder.add(SKOS.NOTATION, term.notation)
                builder.add(RDFS.LABEL, term.label)
                if (term.definition)
                    builder.add(DCTERMS.DESCRIPTION, term.definition)
                if (term.comments)
                    builder.add(RDFS.COMMENT, term.comments)
                if (term.examples)
                    builder.add(SKOS.EXAMPLE, term.examples)
                if (term.organiser) {
                    def coll = lookup[term.organiser]
                    if (coll) {
                        builder.add(SKOS.MEMBER, coll.iri)
                    }
                }
                if (term.issued)
                    builder.add(DCTERMS.ISSUED, valueFactory.createLiteral(term.issued, XMLSchema.DATE))
                if (term.status)
                    builder.add(ALA.STATUS, term.status)
                if (term.iri && term.version && term.iri != term.version)
                    builder.add(OWL.SAMEAS, term.version)
                if (term.csvName)
                    builder.add(ALA.CSV_NAME, term.csvName)
                if (term.jsonName)
                    builder.add(ALA.JSON_NAME, term.jsonName)
                if (term.solrName)
                    builder.add(ALA.SOLR_NAME, term.solrName)
               builder.add(DCTERMS.SOURCE, term.dwc ? dwcSource : alaSource)
            }
            if (complete) {
                builder.subject(term.version)
                builder.add(RDF.TYPE, term.type)
                builder.add(SKOS.NOTATION, term.notation)
                builder.add(RDFS.LABEL, term.label)
                if (term.definition)
                    builder.add(DCTERMS.DESCRIPTION, term.definition)
                if (term.comments)
                    builder.add(RDFS.COMMENT, term.comments)
                if (term.examples)
                    builder.add(SKOS.EXAMPLE, term.examples)
                if (term.issued)
                    builder.add(DCTERMS.ISSUED, valueFactory.createLiteral(term.issued, XMLSchema.DATE))
                if (term.status)
                    builder.add(ALA.STATUS, term.status)
                term.replaces.each { rep -> builder.add(DCTERMS.REPLACES, rep) }
                if (term.iri && term.version && term.iri != term.version)
                    builder.add(OWL.SAMEAS, term.iri)
            }
        }
        return builder.build()
    }
}
