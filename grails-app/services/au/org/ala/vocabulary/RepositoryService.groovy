package au.org.ala.vocabulary


import au.org.ala.vocabulary.model.Categorisation
import au.org.ala.vocabulary.model.Context
import au.org.ala.vocabulary.model.Resource
import org.apache.commons.jcs.JCS
import org.apache.commons.jcs.access.CacheAccess
import org.eclipse.rdf4j.model.*
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.XMLSchema
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.repository.RepositoryConnection
import org.eclipse.rdf4j.repository.RepositoryResult
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema

import javax.annotation.PostConstruct

/**
 * Retrieve (and maybe store, why not?) information from the repository.
 * <p>
 * The repository service contains a number of other useful bits and pieces
 * and caches results.
 */
class RepositoryService {
    static RESOURCE_REGION = 'resource'

    static PREFIX = """
    PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
    PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
    PREFIX dcterms: <http://purl.org/dc/terms/>
    PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#> 
    PREFIX ala: <${ALA.NAMESPACE}>
    PREFIX format: <${Format.NAMESPACE}>
"""

    static SORT = """
    OPTIONAL { ?resource skos:prefLabel ?pl }
    OPTIONAL { ?resource rdfs:label ?rl }
    OPTIONAL { ?resource dcterms:title ?dl }
    OPTIONAL { ?resource skos:notation ?nl }
    BIND (LCASE(COALESCE(?pl, ?rl, ?dl, ?tl, afn:localname(?resource))) AS ?sort)
"""

    def repositoryBean

    /** The namespace cache */
    Map<String, Namespace> namespaces
    /** The resource cache */
    CacheAccess<IRI, Resource> resources = JCS.getInstance(RESOURCE_REGION)
    /** The value factory */
    ValueFactory valueFactory = SimpleValueFactory.instance
    /** The last modified date for caching */
    Date lastModified

    /**
     * Initialise any cached information
     *
     * @return
     */
    @PostConstruct
    def init() {
        buildNamespaces()
        lastModified = new Date()
    }

    /**
     * (Re)build the namespace map from the repository
     */
    def buildNamespaces() {
        RepositoryConnection connection = repositoryBean.connection
        try {
            def newNamespaces = [:]
            def nss = connection.getNamespaces()
            while (nss.hasNext()) {
                def ns = nss.next()
                if (ns.prefix) {
                    newNamespaces[ns.prefix] = ns
                    newNamespaces[ns.name] = ns
                }
            }
            namespaces = newNamespaces
        } finally {
            connection.close()
        }
    }

    /**
     * Empty any caches
     */
    def clearCaches() {
        lastModified = new Date()
        resources.clear()
        buildNamespaces()
    }

    IRI createIRI(String iri) {
        return valueFactory.createIRI(iri)
    }

    /**
     * Get the context associated with a resource.
     * <p>
     * The context contains additional information associated with the statements in the resource
     *
     * @param resource The resource
     * @param categorisation The categorisation
     *
     * @return The context
     */
    Context getContext(Resource resource, Categorisation categorisation = null) {
        return getContext([ resource ], categorisation)
    }

    /**
     * Get the context associated with a list of resources.
     * <p>
     * The context contains additional information associated with the statements in the resources.
     * If a resource has an embedded predicate, then add that too.
     *
     * @param list The resources
     * @param categorisation The categorisation
     *
     * @return The context
     */
    Context getContext(Collection<Resource> resources, Categorisation categorisation = null) {
        Set<IRI> base = resources.collect({ r -> r.iri }) as Set
        Set<IRI> seen = [] as Set
        Set<String> nsUsed = [RDF.NAMESPACE, XMLSchema.NAMESPACE] as Set
        Map<IRI, Resource> entries = [:]
        Queue<IRI> workQueue = new ArrayDeque<>(base)
        if (categorisation) {
            workQueue.addAll(categorisation.categorisation.keySet())
            workQueue.addAll(categorisation.categories)
        }
        while (!workQueue.empty) {
            IRI iri = workQueue.remove()
            if (seen.contains(iri))
                continue
            seen.add(iri)
            nsUsed.add(iri.namespace)
            def resource = getResource(iri)
            if (!base.contains(iri))
                entries[iri] = resource
            resource.statements.each { s ->
                workQueue.add(s.predicate)
                if (s.object in IRI && !seen.contains(s.object)) {
                    def pred = getResource(s.predicate)
                    if (base.contains(iri) || pred.hasValue(Format.EMBED, true))
                        workQueue.add(s.object as IRI)
                }
                if (s.object in Literal && (s.object as Literal).datatype)
                    workQueue.add((s.object as Literal).datatype)
            }
        }
        def nsp = namespaces.findAll { ns -> nsUsed.contains(ns.value.name) }
        return new Context(nsp, entries)
    }

    /**
     * Get the information about a resource
     *
     * @param iri The resource IRI
     *
     * @return A resource that has collected statements about the resource IRI
     */
    Resource getResource(IRI iri) {
        def resource = resources.get(iri)
        if (!resource) {
            RepositoryConnection connection = repositoryBean.connection
            try {
                resource = this.get(iri, connection, true)
            } finally {
                connection.close()
            }
        }
        return resource
    }

    /**
     * Get a resource from another resource.
     *
     * @param resource The resource
     * @param predicates The possible predicates
     *
     * @return Either an object resource matching a statement from (resource, predicate, object) or null for not found
     */
    Resource getResource(Resource resource, IRI... predicates) {
        Value found = resource.getStatement(predicates)
        if (found && found in IRI)
            return this.getResource(found)
        return null
    }

    /**
     * List the resources associated with a type
     *
     * @param type The type
     * @param start The start position
     * @param rows The number of rows to retrieve
     * @return
     */
    def listResources(IRI type, int start, int rows) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            TupleQuery countQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT (count(?resource) as ?count) WHERE { ?resource a <${type.stringValue()}>. }")
            int count = (countQuery.evaluate()?.next()?.getValue('count') as Literal)?.intValue() ?: 0
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, "${PREFIX} SELECT DISTINCT ?resource WHERE { ?resource a <${type.stringValue()}>. ${SORT} } ORDER BY ?sort OFFSET ${start} LIMIT ${rows}")
            TupleQueryResult result = query.evaluate()
            List<Resource> resources = new ArrayList<>(Math.min(count, rows))
            while (result.hasNext()) {
                IRI iri = result.next().getValue('resource') as IRI
                resources << this.get(iri, connection, true)
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    /**
     * List the referenes to a resource
     * <p>
     * Since there may be multiple references, this is designed to return pages
     * </p>
     *
     * @param iri The resource iri
     * @param start The start position
     * @param rows The number of rows to retrieve
     * @return
     */
    def listReferences(IRI iri, int start, int rows) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            def where =  "{ ?resource ?_predicate <${iri.stringValue()}>. } UNION { ?resource <${iri.stringValue()}> ?_value. }"
            TupleQuery countQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT (count(?resource) as ?count) WHERE { ${where} } ")
            int count = (countQuery.evaluate()?.next()?.getValue('count') as Literal)?.intValue() ?: 0
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, "${PREFIX} SELECT DISTINCT ?resource WHERE { ${where} ${SORT} } ORDER BY ?sort OFFSET ${start} LIMIT ${rows}")
            TupleQueryResult result = query.evaluate()
            List<Resource> resources = new ArrayList<>(Math.min(count, rows))
            while (result.hasNext()) {
                IRI resource = result.next().getValue('resource') as IRI
                resources << this.get(resource, connection, true)
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    /**
     * List the terms in a set of vocabularies
     *
     * @param iris The DwC terms the vocabularies are for
     *
     * @return
     */
    def listTerms(List<IRI> iris) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            int count = 0
            def values = ''
            def where = "?resource skos:inScheme ?vocab. ?vocab a ala:DwCVocabulary. "
            if (iris) {
                values = 'VALUES ?dwc { ' + iris.collect({ '<' + it.stringValue() + '>' }).join(' ') + '}'
                where = where + '?vocab ala:forTerm ?dwc.'
            }
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, "${PREFIX} SELECT DISTINCT ?resource WHERE { ${values} ${where} ${SORT} } ORDER BY ?sort")
            TupleQueryResult result = query.evaluate()
            List<Resource> resources = new ArrayList<>(64)
            while (result.hasNext()) {
                IRI resource = result.next().getValue('resource') as IRI
                resources << this.get(resource, connection, true)
                count++
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    /**
     * Search for a resource that contains text
     *
     * @param text The text to search for
     * @param start The start position
     * @param rows The number of rows to retrieve
     *
     * @return The count and the resources. The resources are annotated with the score and snippet from the search
     */
    def search(String text, int start, int rows) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            def prefix = 'PREFIX search: <http://www.openrdf.org/contrib/lucenesail#>'
            def search = "?resource search:matches [ search:query \"${text}\"; search:score ?score1; search:snippet ?snippet1 ] ."
            TupleQuery countQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, "${prefix} SELECT (count(?resource) as ?count) WHERE { SELECT DISTINCT ?resource WHERE { $search } }")
            TupleQueryResult countResult = countQuery.evaluate()
            if (!countResult || !countResult.hasNext()) {
                return [count: 0, resources: []]
            }
            int count = (countResult.next()?.getValue('count') as Literal)?.intValue() ?: 0
            TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, "${prefix} SELECT ?resource (MAX(?score1) AS ?score) (GROUP_CONCAT(?snippet1; SEPARATOR=\" <BR> \") AS ?snippet) WHERE { SELECT ?resource ?score1 ?snippet1 WHERE { $search } } GROUP BY ?resource ORDER BY DESC(?score) OFFSET ${start} LIMIT ${rows}")
            TupleQueryResult result = query.evaluate()
            List<Resource> resources = new ArrayList<>(Math.min(count, rows))
            while (result.hasNext()) {
                BindingSet binding = result.next()
                IRI iri = binding.getValue('resource') as IRI
                Resource copy = this.get(iri, connection, true).clone()
                copy.addStatement(connection.valueFactory.createStatement(iri, LuceneSailSchema.SCORE, binding.getValue('score')))
                copy.addStatement(connection.valueFactory.createStatement(iri, LuceneSailSchema.SNIPPET, binding.getValue('snippet')))
                resources << copy
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    /**
     * Run a simple SPARQL query
     *
     * @param query The query
     *
     * @return A list of all resources returned by the query
     */
    def query(String query) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            int count = 0
            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query)
            TupleQueryResult result = q.evaluate()
            List<Resource> resources = new ArrayList<>()
            def names = result.bindingNames
            while (result.hasNext()) {
                def binding = result.next()
                names.each { name ->
                    def value = binding.getValue(name)
                    if (value in IRI) {
                        resources << this.get(value as IRI, connection, true)
                        count++
                    }
                }
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    /**
     * Run a paged SPARQL query
     *
     * @param query The query
     * @param countQuery The query to get the count of values
     * @param offset The start position
     * @param max The maximum number of results to return
     *
     * @return A list of all resources returned by the query
     */
    def query(String query, String countQuery, int offset, int max) {
        RepositoryConnection connection = repositoryBean.connection
        try {
            TupleQuery cq = connection.prepareTupleQuery(QueryLanguage.SPARQL, countQuery)
            TupleQueryResult countResult = cq.evaluate()
            if (!countResult || !countResult.hasNext())
                return [count: 0, resources: []]
            int count = (countResult.next()?.getValue(countResult.bindingNames[0]) as Literal)?.intValue()
            TupleQuery q = connection.prepareTupleQuery(QueryLanguage.SPARQL, query + " OFFSET ${offset} LIMIT ${max}" )
            TupleQueryResult result = q.evaluate()
            List<Resource> resources = new ArrayList<>(Math.min(count, max))
            def names = result.bindingNames
            while (result.hasNext()) {
                def binding = result.next()
                names.each { name ->
                    def value = binding.getValue(name)
                    if (value in IRI) {
                        resources << this.get(value as IRI, connection, true)
                    }
                }
            }
            return [count: count, resources: resources]
        } finally {
            connection.close()
        }
    }

    protected Resource get(IRI iri, RepositoryConnection connection, boolean extend) {
        def resource = resources.get(iri)
        if (!resource)  {
            RepositoryResult<Statement> smts = connection.getStatements(iri, (IRI) null, null, true)
            resource = new Resource(iri, smts)
            resources.put(iri, resource)
            resource.statements.each { s -> // Get contextual information
                get(s.predicate, connection, true)
                if ((extend) && s.object in IRI)
                    get(s.object as IRI, connection, false)
            }
        }
        return resource
    }
}
