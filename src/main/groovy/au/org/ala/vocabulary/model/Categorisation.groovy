package au.org.ala.vocabulary.model

import au.org.ala.vocabulary.Format
import org.eclipse.rdf4j.model.IRI

/**
 * A categorisation of a set of properties.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
class Categorisation {
    /** The categorisation */
    Map<IRI, List<IRI>> categorisation
    /** The ordered list of categories */
    List<IRI> categories

    Categorisation() {
        this.categorisation = [:]
        this.categories = null
    }

    /**
     * Sort the categories that we have into priority order
     *
     * @param ResourceGetter Get the resource associated wiuth
      */
    def sortCategories(Closure<Resource> resourceGetter) {
        categories = categorisation.keySet() as List
        categories.sort { c1, c2 ->
            def v1 = resourceGetter.call(c1)
            def v2 = resourceGetter.call(c2)
            def p1 = v1?.getStatement(Format.PRIORITY)?.intValue() ?: 0
            def p2 = v2?.getStatement(Format.PRIORITY)?.intValue() ?: 0
            return p2 - p1
        }
    }

    def add(IRI category, IRI property) {
        def list = categorisation[category]
        if (list == null) {
            list = []
            categorisation[category] = list
        }
        if (!list.contains(property))
            list << property
    }
}
