package au.org.ala.vocabulary.model

import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.grails.core.io.ResourceLocator

/**
 * Resolve links according to the asset pipeline.
 * <p>
 * If a value looks like an asset, then it will be resolved against the pipeline
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
class AssetResolver implements Resolver {
    Closure<String> path
    ValueFactory factory

    @Override
    Value resolve(IRI resource) {
        return resource
    }

    @Override
    Value resolve(Literal resource) {
        def resolved = path.call(resource.stringValue())
        if (!resolved)
            return resource
        return factory.createIRI(resolved)
    }

    @Override
    Value resolve(Value resource) {
        return resource
    }
}
