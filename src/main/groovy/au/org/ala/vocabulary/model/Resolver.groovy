package au.org.ala.vocabulary.model

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value

/**
 * A resolver converts some sort of asset description into either
 * a literal value or a URL of some sort.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
interface Resolver {
    /**
     * Resolve an IRI into a resource.
     *
     * @param resource The resource to resolve
     *
     * @return Either the resource itself or a resolved version of the resource
     */
    Value resolve(IRI resource)

    /**
     * Resolve a literal into a resource.
     *
     * @param resource The resource to resolve
     *
     * @return Either the resource itself or a resolved version of the resource
     */
    Value resolve(Literal resource)

    /**
     * Resolve a value into a resource.
     *
     * @param resource The resource to resolve
     *
     * @return Either the resource itself or a resolved version of the resource
     */
    Value resolve(Value resource)
}
