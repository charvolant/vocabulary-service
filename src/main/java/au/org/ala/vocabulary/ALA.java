package au.org.ala.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for the ALA vocabulary.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
public class ALA {
    /**
     * The value factory for this vocabulary
     */
    private static final ValueFactory FACTORY = SimpleValueFactory.getInstance();

    /**
     * The Format namespace: http://www.ala.org.au/terms/1.0/
     */
    public static final String NAMESPACE = "http://www.ala.org.au/terms/1.0/";

    /**
     * The recommended prefix for the ALA namespace: "ala"
     */
    public static final String PREFIX = "ala";

    /**
     * An immutable {@link Namespace} constant that represents the Format namespace.
     */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    /* classes */

    /**
     * The ala:DwCVocabulary class
     */
    public static final IRI DWC_VOCABULARY = FACTORY.createIRI(NAMESPACE, "DwCVocabulary");

    /* properties */

    /**
     * The ala:DwCVocabulary class
     */
    public static final IRI FOR_TERM = FACTORY.createIRI(NAMESPACE, "forTerm");
}

