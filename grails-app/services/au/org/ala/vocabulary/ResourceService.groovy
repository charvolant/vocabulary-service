package au.org.ala.vocabulary

import au.org.ala.util.ResourceUtils
import au.org.ala.vocabulary.model.Resource
import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal

class ResourceService implements GrailsConfigurationAware {
    def categorisationService
    def repositoryService

    /** The sources for labels */
    List<List<IRI>> labelSources
    /** The sources for titles */
    List<List<IRI>> titleSources
    /** The sources for descriptions */
    List<List<IRI>> descriptionSources

    static final TYPE_QUERY = "SELECT DISTINCT ?type WHERE { ?_resource a ?type. }"
    static final TYPE_COUNT_QUERY = "SELECT (count(?type) as ?count) WHERE { ${TYPE_QUERY} }"

    @Override
    void setConfiguration(Config config) {
        def toIRI = { list1 -> list1.collect { list2 -> list2.collect { iri -> repositoryService.valueFactory.createIRI(iri) } } }
        labelSources = toIRI(config.vocabulary.label.sources)
        titleSources = toIRI(config.vocabulary.title.sources)
        descriptionSources = toIRI(config.vocabulary.description.source)s
    }

    /**
     * List the types of resource available
     *
     * @return A list of types
     */
    def listTypes(int offset, int max) {
        def list = repositoryService.query(TYPE_QUERY, TYPE_COUNT_QUERY, offset, max)
        list.categorisation = categorisationService.categorise(list.resources)
        list.context = repositoryService.getContext(list.resources, list.categorisation)
        return list
    }

    /**
     * List resources of a particular type
     *
     * @param type The type to list
     * @param offset The offset into the query
     * @param max The maximum number of elements to retrieve
     *
     * @return An object containing the list, the total count, a context and a categorisation
     */

    def listResources(String type, int offset, int max) {
        def typeIri = repositoryService.createIRI(type)
        def list = repositoryService.listResources(typeIri, offset, max)
        list.categorisation = categorisationService.categorise(list.resources)
        list.context = repositoryService.getContext(list.resources, list.categorisation)
        return list
    }

    /**
     * List references to a resource
     *
     * @param iri Resource to reference
     * @param offset The offset into the query
     * @param max The maximum number of elements to retrieve
     *
     * @return An object containing the list, the total count, a context and a categorisation
     */

    def listReferences(String iri, int offset, int max) {
        def rIri = repositoryService.createIRI(iri)
        def list = repositoryService.listReferences(rIri, offset, max)
        list.categorisation = categorisationService.categorise(list.resources)
        list.context = repositoryService.getContext(list.resources, list.categorisation)
        return list
    }

    /**
     * Search for resources with text
     *
     * @param text The text to search for
     * @param offset The offset into the query
     * @param max The maximum number of elements to retrieve
     *
     * @return An object containing the list, the total count, a context and a categorisation
     */

    def search(String text, int offset, int max) {
        def list = repositoryService.search(text, offset, max)
        list.categorisation = categorisationService.categorise(list.resources)
        list.context = repositoryService.getContext(list.resources, list.categorisation)
        return list
    }

    /**
     * List vocabulary terms
     *
     * @param vocs The vocabularies to retrieve, null for all
     *
     * @return An object containing the list, the total count, a context and a categorisation
     */

    def listTerms(List<String> vocs) {
        def iris = vocs.collect { repositoryService.valueFactory.createIRI(it) }
        def terms = repositoryService.listTerms(iris)
        terms.categorisation = categorisationService.categorise(terms.resources)
        terms.context = repositoryService.getContext(terms.resources, terms.categorisation)
        return terms
    }

    /**
     * Get a resource
     *
     * @param iri The resource IRI
     *
     * @return An object containing the resource, a context and a categorisation
     */
    def getResource(String iri) {
        IRI riri = repositoryService.createIRI(iri)
        def resource = repositoryService.getResource(riri)
        def categorisation = categorisationService.categorise(resource)
        def context = repositoryService.getContext(resource, categorisation)
        return [resource: resource, context: context, categorisation: categorisation]
    }

    /**
     * Get a suitable label (short descriptive text) for a resource as a JSON-LD map
     *
     * @param resource The resource as
     * @param locale The preferred locale
     *
     * @return A label, or none for not present
     */
    def getLabel(Resource resource, Locale locale) {
        def label = getText(resource, labelSources, locale)
        if (label)
            return label
        def iri = resource.iri
        return contract(iri) ?: iri.localName
    }

    /**
     * Get a suitable title (short headline text) for a resource
     *
     * @param resource The resource
     * @param locale The preferred locale
     *
     * @return A title, or none for not present
     */
    def getTitle(Resource resource, Locale locale) {
        return getText(resource, titleSources, locale)
    }

    /**
     * Get a suitable description (long descriptive text) for a resource
     *
     * @param resource The resource
     * @param context The resource context
     * @param locale The preferred locale
     *
     * @return A description, or none for not present
     */
    def getDescription(Resource resource, Locale locale) {
        return getText(resource, descriptionSources, locale)
    }

    /**
     * Search for a suitable property.
     * <p>
     * Groups of sources are searched in order first for a language tag match, then for a language match and then for a value.
     * If one is not found, then the next group of sources is tried.
     * </p>
     * <p>
     * For example, if the sources are <code>[[dcterms:title, >dc:title]]</code>, the locale is 'fr-CA' and the possible values are
     * <code>dcterms:title = ['chercheuse'@fr, 'researcher' ] dc:title = ['chercheure'@fr-CA ]</code> will result in 'chercheure'.
     * If the sources are <code>[[skos:prefLabel], [rdfs:label]]</code>, the locale is 'fr-CA' and the possible values
     * are <code>skos:prefLabel = ['tofu'@fr, 'tahu'], rdfs:label = ['toffu'@fr-CA ]</code> with result in 'tofu'
     * </p>
     *
     * @param resource The resource
     * @param sources The sources, a list of lists of
     * @param locale The locale to use
     *
     * @return A suitable value or null for not found
     */
    protected def getText(Resource resource, List<List<String>> sources, Locale locale) {
        if (!resource)
            return null
        def lt = locale.toLanguageTag()
        def ln = locale.language
        def finders = [{ it in Literal && it.language.present && it.language.get() == lt }, { it in Literal && it.language.present && it.language.get() == ln }, { it in Literal && !it.language.present } ]
        for (List<String> group: sources) {
            for (Closure<Boolean> finder: finders) {
                for (String source : group) {
                    def siri = repositoryService.valueFactory.createIRI(source)
                    def labels = resource.getStatements(siri)
                    if (!labels)
                        continue
                    Literal lab = (labels in Iterable) ? labels.find(finder) : (finder(labels) ? labels : null)
                    if (lab)
                        return lab.stringValue()
                }
            }
        }
        return null
    }

    /**
     * Make a namespace contracte version of an IRI
     *
     * @param iri The IRI
     *
     * @return The contracted version
     */
    def contract(IRI iri) {
        def ns = repositoryService.namespaces.values().find { n -> iri.namespace == n.name }
        return ns ? ns.prefix + ':' + iri.localName : null
    }

}
