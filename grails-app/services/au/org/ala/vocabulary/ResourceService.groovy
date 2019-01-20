package au.org.ala.vocabulary


import org.eclipse.rdf4j.model.IRI

class ResourceService {
    def categorisationService
    def repositoryService

    static final TYPE_QUERY = "SELECT DISTINCT ?type WHERE { ?_resource a ?type. }"
    static final TYPE_COUNT_QUERY = "SELECT (count(?type) as ?count) WHERE { ${TYPE_QUERY} }"

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
}
