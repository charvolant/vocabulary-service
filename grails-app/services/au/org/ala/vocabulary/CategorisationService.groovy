package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.Categorisation
import au.org.ala.vocabulary.model.Context
import au.org.ala.vocabulary.model.Resource
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.QueryLanguage
import org.eclipse.rdf4j.query.TupleQuery
import org.eclipse.rdf4j.query.TupleQueryResult
import org.eclipse.rdf4j.repository.RepositoryConnection

import javax.annotation.PostConstruct

/**
 * Provide categorisation services for colelctions of RDF triples.
 */
class CategorisationService {
    def repositoryBean
    def repositoryService
    def grailsApplication

    /** The default category */
    Resource defaultCategory
    /** The category map */
    Map<IRI, Resource> categoryMap
    /** The order to check properties */
    List<IRI> propertyOrder
    /** The known property set */
    Set<IRI> knownProperties

    static COLLECT_CATEGORIES = '''
    PREFIX format: <http://www.ala.org.au/format/1.0/>
    SELECT ?category WHERE {
        ?category a format:Category
    }
'''

    static COLLECT_PROPERTIES = '''
    PREFIX format: <http://www.ala.org.au/format/1.0/>
    PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    SELECT ?property ?category ?before ?after WHERE {
        ?property format:category ?category.
        OPTIONAL { ?property format:comesBefore ?before }
        OPTIONAL { ?property format:comesAfter ?after }
    }
'''

    @PostConstruct
    def init() {
        buildCategories()
    }

    /**
     * Build the category maps
     */
    protected def buildCategories() {
        RepositoryConnection connection = repositoryBean.connection;
        def categoryMap = [:] // Build using local variables and update at the end
        def propertyOrder = []
        def defaultCategory
        def addBefore = { relations, before, after ->
            def l = relations[before]
            if (!l) {
                l = [] as Set
                relations[before] = l
            }
            l << after
            relations
        }
        def propertyRelations = [:]

        try {
            // Collect category definitions and give them to a category context
            defaultCategory = repositoryService.get(repositoryService.createIRI(grailsApplication.config.format.defaultCategory), connection, true)

            // Collect categories for properties
            def query = connection.prepareTupleQuery(QueryLanguage.SPARQL, COLLECT_PROPERTIES)
            def result = query.evaluate()
            while (result.hasNext()) {
                BindingSet binding = result.next()
                IRI property = binding.getValue('property') as IRI
                IRI category = binding.getValue('category') as IRI
                IRI before =  binding.getValue('before') as IRI
                IRI after = binding.getValue('after') as IRI
                if (category)
                    categoryMap[property] = repositoryService.get(category, connection, true)
                if (before)
                    propertyRelations = addBefore(propertyRelations, property, before)
                if (after)
                    propertyRelations = addBefore(propertyRelations, after, property)
                if (!propertyRelations[property])
                    propertyRelations[property] = [] as Set
            }
        } finally {
            connection.close()
        }
        // Build transitive closure
        boolean changed = true
        while (changed) {
            changed = false
            propertyRelations.each { IRI property, Set<IRI> before ->
                Set<IRI> original = before.clone()
                original.each { after ->
                    Set downstream = propertyRelations[after]
                    if (downstream)
                        before.addAll(downstream)
                }
                changed = changed || original.size() != before.size()
            }
        }
        // Topological sort
        def properties = propertyRelations.keySet()
        while (!properties.isEmpty()) {
            def candidate = null
            def ci = properties.iterator()
            while (candidate == null && ci.hasNext()) {
                candidate = ci.next()
                def after = properties.any { p -> p != candidate && propertyRelations[p].contains(candidate) }
                if (after)
                    candidate = null
            }
            if (!candidate) {
                candidate = properties.find()
                log.error("Unable to find candidate from ${properties} choosing random element ${candidiate}")
            }
            propertyOrder << candidate
            properties.remove(candidate)
        }
        this.categoryMap = categoryMap
        this.defaultCategory = defaultCategory
        this.propertyOrder = propertyOrder
        this.knownProperties = propertyOrder as Set
    }

    /**
     * Clear and rebuild ay changed information
     */
    def clearCaches() {
        buildCategories()
    }

    /**
     * Builds a categorisation for a resource.
     *
     * @param resource The resource
     *
     * @return The categorisation
     */
    def categorise(Resource resource) {
        def categorisation = new Categorisation()
        categorise(resource, categorisation)
        categorisation.sortCategories({ p -> repositoryService.getResource(p)} )
        return categorisation
    }

    /**
     * Builds a categorisation for a list of resources.
     *
     * @param resource The resource
     *
     * @return The categorisation
     */
    def categorise(Collection<Resource> resources) {
        def categorisation = new Categorisation()
        resources.each { resource -> categorise(resource, categorisation) }
        categorisation.sortCategories({ p -> repositoryService.getResource(p) } )
        return categorisation
    }

    /**
     * Builds a categorisation for a resource.
     *
     * @param resource The resource
     * @param categorisation The categorisation to build
     */
    protected def categorise(Resource resource, Categorisation categorisation) {
        propertyOrder.each { property ->
            if (resource.predicates.contains(property)) {
                def category = categoryMap[property] ?: defaultCategory
                categorisation.add(category.iri, property)
            }
        }
        resource.predicates.each { property ->
            if (!knownProperties.contains(property)) {
                def category = categoryMap[property] ?: defaultCategory
                categorisation.add(category.iri, property)
            }
        }
    }

}
