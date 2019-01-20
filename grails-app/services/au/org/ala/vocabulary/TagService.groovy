package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.Resolver
import au.org.ala.vocabulary.model.Resource
import org.apache.commons.jcs.JCS
import org.apache.commons.jcs.access.CacheAccess
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.vocabulary.DC
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.SKOS
import org.eclipse.rdf4j.spin.function.spif.For

/**
 * A service that manages tags.
 * <p>
 * Tags are concepts that can be represented by a graphical tag.
 */
class TagService {
    /** The cahce region for tag classes */
    static TAG_CLASS_REGION = 'tag-class'
    /** The tag query */
    static TAG_QUERY = 'select DISTINCT ?tag WHERE { ?tag a <' + Format.CONCEPT.stringValue() + '> }'

    def repositoryService

    /** The tag class cache */
    CacheAccess<IRI, String> tagClasses = JCS.getInstance(TAG_CLASS_REGION)

    /**
     * Empty any caches
     */
    def clearCaches() {
        this.tagClasses.clear()
    }

    /**
     * Create a css class for a particular concept/tag.
     * <p>
     * The css class is drawn from the vocabulary (scheme) if one is available
     * and the name of the tag.
     *
     * @param tag The tag to produce a class for
     *
     * @return The tag css class
     */
    def tagClass(Resource tag) {
        String tagClass = tagClasses.get(tag.iri)
        if (!tagClass) {
            String base = tag.hasType(Format.LANGUAGE) ? 'language' : (tag.hasType(Format.TERM) ? 'term' :'tag')
            Resource scheme = repositoryService.getResource(tag, SKOS.IN_SCHEME)
            String schemeClass = scheme?.findTerm(null, SKOS.NOTATION) ?: scheme?.iri.localName
            String termClass = tag.findTerm(null, SKOS.NOTATION) ?: tag.iri.localName
            schemeClass = schemeClass?.toLowerCase().replaceAll(/[^a-z]/, '')
            termClass = termClass?.toLowerCase().replaceAll(/[^a-z]/, '')
            tagClass = schemeClass ? base + '-' + schemeClass + '-' + termClass : base + '-' + termClass
            tagClasses.put(tag.iri, tagClass)
        }
        return tagClass
    }

    /**
     * Get summary tag information for each tag
     */
    def tags(Locale locale, Map<IRI, Resolver> resolvers) {
        def ts = repositoryService.query(TAG_QUERY)
        return ts.resources.inject([]) { List tl, Resource tag ->
            def tg = [:]
            String tagClass = tagClass(tag)
            boolean language = tag.hasType(Format.LANGUAGE)
            boolean term = tag.hasType(Format.TERM)
            String tagType = language ? 'tag-language' : (term ? 'tag-term' : 'tag-concept')
            tg.tagClass = tagClass
            Resource scheme = repositoryService.getResource(tag, SKOS.IN_SCHEME)
            tg.schemeIRI = scheme?.iri?.stringValue()
            tg.scheme = scheme?.iri?.localName
            Resource dwcTerm = scheme ? repositoryService.getResource(scheme, ALA.FOR_TERM) : null
            tg.dwcTermIRI = dwcTerm?.iri?.stringValue()
            tg.dwcTerm = dwcTerm?.iri?.localName
            tg.vocabulary = tg.dwcTerm ?: tg.scheme ?: 'unknown'
            tg.backgroundColor = tag.findTerm(locale, Format.BACKGROUND_COLOR) ?: scheme?.findTerm(null, Format.BACKGROUND_COLOR)
            tg.textColor = tag.findTerm(locale, Format.TEXT_COLOR) ?: scheme?.findTerm(locale, Format.TEXT_COLOR)
            def icon = repositoryService.getResource(tag, Format.ICON)
            tg.width = tag.findTerm(locale, Format.WIDTH) ?: icon?.findTerm(locale, Format.WIDTH)
            tg.height = tag.findTerm(locale, Format.HEIGHT) ?: icon?.findTerm(locale, Format.HEIGHT)
            tg.label = tag.findTerm(locale, SKOS.PREF_LABEL, RDFS.LABEL, SKOS.NOTATION)
            tg.labels = tag.findTerms(locale, SKOS.NOTATION, SKOS.PREF_LABEL, RDFS.LABEL, SKOS.ALT_LABEL)
            tg.title = tag.findTerm(locale, DCTERMS.TITLE, DC.TITLE)
            tg.description = tag.findTerm(locale, DCTERMS.DESCRIPTION, DC.DESCRIPTION)
            tg.id = tag.iri.stringValue()
            tg.localName = tag.iri.localName
            tg.key = tag.findTerm(locale, SKOS.NOTATION, SKOS.PREF_LABEL, RDFS.LABEL)
            if (tg.key == tg.label && tg.title && tg.title.length() < 32)
                tg.label = tg.title
            if (icon) {
                def src = null
                Value asset = icon.getStatement(Format.ASSET)
                if (asset) {
                    Resolver resolver = resolvers.get(Format.ASSET)
                    src = resolver?.resolve(asset)?.stringValue() ?: asset.stringValue()
                }
                if (!src)
                    src = icon.iri.stringValue()
                tg.icon = src
                tagType = 'tag-icon'
            }
            tg.cssClass = [ 'rdf-resource', tagType, tagClass ]
            def cssClass = tag.findTerm(locale, Format.CSS_CLASS) ?: scheme?.findTerm(locale, Format.CSS_CLASS)
            if (cssClass)
                tg.cssClass << cssClass
            tl << tg
            tl
        }
    }
}
