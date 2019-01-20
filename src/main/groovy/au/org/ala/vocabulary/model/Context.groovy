package au.org.ala.vocabulary.model

import com.sun.istack.internal.Nullable
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Namespace
import org.grails.core.io.ResourceLocator

import javax.naming.Name
import java.util.concurrent.ConcurrentHashMap

/**
 * Contextual information for a model.
 * <p>
 * This holds handy things like a mapping from predicates to more complete resource descriptions.
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
class Context {
    /** The namespace map */
    Map<String, Namespace> namespaces;
    /** The resource map (private for concurrency) */
    Map<IRI, Resource> resources
    /** Resolvers for working out what to do with particular predicates */
    Map<IRI, Resolver> resolvers

    Context(Map<String, Namespace> namespaces, Map<IRI, Resource> resources) {
        this.namespaces = namespaces
        this.resources = resources
    }

    /**
     * Get a, preferably namespaced, name for an IRI
     *
     * @param iri The IRI
     *
     * @return The name
     */
    String shortLabel(IRI iri) {
        def ns = namespaces[iri.namespace]
        if (ns?.prefix)
            return "${ns.prefix}:${iri.localName}"
        else
            return iri.stringValue()
    }

    /**
     * Get a, preferably namespaced, name for a resource
     *
     * @param resource The resource
     *
     * @return The name
     */
    String shortLabel(Resource resource) {
        return shortLabel(resource.iri)
    }

    /**
     * Get a resolver for this predicate
     *
     * @param predicate The predicate
     *
     * @return The resolver, or null for none
     */
    Resolver resolver(IRI predicate) {
        return resolvers?.get(predicate)
    }

}
