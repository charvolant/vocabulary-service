package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.Resolver
import au.org.ala.vocabulary.model.Resource
import org.apache.commons.jcs.JCS
import org.apache.commons.jcs.access.CacheAccess
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.vocabulary.DC
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.SKOS

import javax.annotation.PostConstruct

/**
 * A service that manages tags.
 * <p>
 * Tags are concepts that can be represented by a graphical tag.
 */
class TagService {
    /** The cahce region for tag classes */
    static TAG_CLASS_REGION = 'tag-class'
    /** The cahce region for tag definitions */
    static TAG_DEFINITION_REGION = 'tag-definition'
    /** The tag class query */
    static TAG_CLASS_QUERY = 'select DISTINCT ?clazz WHERE { ?tag a <' + Format.CONCEPT.stringValue() + '>. ?tag a ?clazz. ?clazz <' + RDFS.SUBCLASSOF.stringValue() + '> <' + Format.CONCEPT.stringValue() + '> }'
    /** The tag scheme query */
    static TAG_SCHEME_QUERY = 'select DISTINCT ?scheme WHERE { ?tag a <' + Format.CONCEPT.stringValue() + '>. ?tag <' + SKOS.IN_SCHEME.stringValue() + '> ?scheme }'
    /** The tag query */
    static TAG_QUERY = 'select DISTINCT ?tag WHERE { ?tag a <' + Format.CONCEPT.stringValue() + '> }'

    def repositoryService

    /** The tag class cache */
    CacheAccess<IRI, String> tagClasses = JCS.getInstance(TAG_CLASS_REGION)
    /** The tag defintion */
    CacheAccess<IRI, Map<Locale, Object>> tagDefinitions = JCS.getInstance(TAG_DEFINITION_REGION)
    
    /** The set of concept types */
    Set<IRI> conceptTypes

    /**
     * Empty any caches
     */
    def clearCaches() {
        tagClasses.clear()
        tagDefinitions.clear()
        intiialize()
    }

    /**
     * Concstruct the list of concept types
     */
    @PostConstruct
    def intiialize() {
        conceptTypes = [] as Set
        conceptTypes << Format.CONCEPT
        repositoryService.query(TAG_CLASS_QUERY).resources.each { conceptTypes << it.iri }
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
            String base = 'tag'
            Resource scheme = repositoryService.getResource(tag, SKOS.IN_SCHEME)
            String schemeClass = scheme?.findTerm(null, SKOS.NOTATION) ?: scheme?.iri?.localName
            String termClass = tag.findTerm(null, SKOS.NOTATION) ?: tag.iri.localName
            schemeClass = schemeClass?.toLowerCase()?.replaceAll(/[^a-z]/, '')
            termClass = termClass.toLowerCase().replaceAll(/[^a-z]/, '')
            tagClass = schemeClass ? base + '-' + schemeClass + '-' + termClass : base + '-' + termClass
            tagClasses.put(tag.iri, tagClass)
        }
        return tagClass
    }
    
    def tagDefinition(Resource resource, Locale locale, Map<IRI, Resolver> resolvers) {
        def defns = tagDefinitions.get(resource.iri)
        if (defns) {
            def defn = defns.get(locale)
            if (defn)
                return defn
        }
        def tag = [:]
        def classes = [ 'rdf-resource', 'tag-text' ] as Set
        tag.tagClass = tagClass(resource)
        classes.add(tag.tagClass)
        def cssClass = resource.findTerm(locale, Format.CSS_CLASS)
        if (cssClass)
            classes.add(cssClass)
        def types = resource.getStatements(RDF.TYPE).findAll { v ->  conceptTypes.contains(v) }
        types.each { type ->
            def typeDef = tagDefinition(repositoryService.getResource(type), locale, resolvers)
            classes.addAll(typeDef.cssClass)
        }
        Resource scheme = repositoryService.getResource(resource, SKOS.IN_SCHEME)
        if (scheme) {
            def schemeDefn = tagDefinition(scheme, locale, resolvers)
            classes.addAll(schemeDefn.cssClass)
            tag.schemeIRI = schemeDefn.id
            tag.scheme = schemeDefn.localName
            Resource dwcTerm = repositoryService.getResource(scheme, ALA.FOR_TERM)
            if (dwcTerm) {
                def termDefn = tagDefinition(dwcTerm, locale, resolvers)
                tag.dwcTermIRI = termDefn?.id
                tag.dwcTerm = termDefn?.localName
            }
        }
        tag.vocabulary = tag.dwcTerm ?: tag.scheme ?: 'unknown'
        tag.backgroundColor = resource.findTerm(locale, Format.BACKGROUND_COLOR)
        tag.textColor = resource.findTerm(locale, Format.TEXT_COLOR)
        def icon = repositoryService.getResource(resource, Format.ICON)
        tag.width = resource.findTerm(locale, Format.WIDTH) ?: icon?.findTerm(locale, Format.WIDTH)
        tag.height = resource.findTerm(locale, Format.HEIGHT) ?: icon?.findTerm(locale, Format.HEIGHT)
        tag.label = resource.findTerm(locale, SKOS.PREF_LABEL, RDFS.LABEL, SKOS.NOTATION)
        tag.labels = resource.findTerms(locale, SKOS.NOTATION, SKOS.PREF_LABEL, RDFS.LABEL, SKOS.ALT_LABEL)
        tag.title = resource.findTerm(locale, DCTERMS.TITLE, DC.TITLE)
        tag.description = resource.findTerm(locale, DCTERMS.DESCRIPTION, DC.DESCRIPTION)
        tag.id = resource.iri.stringValue()
        tag.localName = resource.iri.localName
        tag.key = resource.findTerm(locale, SKOS.NOTATION, SKOS.PREF_LABEL, RDFS.LABEL)
        if (tag.key == tag.label && tag.title && tag.title.length() < 32)
            tag.label = tag.title
        if (icon) {
            def src = null
            Value asset = icon.getStatement(Format.ASSET)
            if (asset) {
                Resolver resolver = resolvers.get(Format.ASSET)
                src = resolver?.resolve(asset)?.stringValue() ?: asset.stringValue()
            }
            if (!src)
                src = icon.iri.stringValue()
            tag.icon = src
            classes.add('tag-icon')
            classes.remove('tag-text')
        }
        tag.cssClass = classes
        if (!defns) {
            defns = [:]
            tagDefinitions.put(resource.iri, defns)
        }
        defns.put(locale, tag)
        return tag
    }

    /**
     * Get summary tag information for each tag
     */
    def tags(Locale locale, Map<IRI, Resolver> resolvers) {
        def tags = []
        conceptTypes.each { IRI tc ->
            def css = tagDefinition(repositoryService.getResource(tc), locale, resolvers)
            tags << css
        }
        def schemes = repositoryService.query(TAG_SCHEME_QUERY).resources
        schemes.each { Resource ts ->
            def css = tagDefinition(ts, locale, resolvers)
            tags << css
        }
        def ts = repositoryService.query(TAG_QUERY).resources
        ts.each { Resource tg ->
            def css = tagDefinition(tg, locale, resolvers)
            tags << css
        }
        return tags
     }
}
