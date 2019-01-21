package au.org.ala.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Language type vocabulary
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2019 Atlas of Living Australia
 */
public class LanguageType {
    /**
     * The value factory for this vocabulary
     */
    private static final ValueFactory FACTORY = SimpleValueFactory.getInstance();

    /**
     * The language type namespace: http://www.ala.org.au/format/1.0/languageTypes
     */
    public static final String NAMESPACE = "http://www.ala.org.au/format/1.0/languageTypes/";

    /**
     * The recommended prefix for the Format namespace: "languageType"
     */
    public static final String PREFIX = "languageType";

    /**
     * The individual language type
     */
    public static final IRI INDIVIDUAL = FACTORY.createIRI(NAMESPACE, "individual");

    /**
     * The macro-language language type
     */
    public static final IRI MACRO_LANGUAGE = FACTORY.createIRI(NAMESPACE, "macrolanguage");

    /**
     * The special language type
     */
    public static final IRI SPECIAL = FACTORY.createIRI(NAMESPACE, "special");

    /**
     * The ancient language type
     */
    public static final IRI ANCIENT = FACTORY.createIRI(NAMESPACE, "ancient");

    /**
     * The constructed language type
     */
    public static final IRI CONSTRUCTED = FACTORY.createIRI(NAMESPACE, "constructed");

    /**
     * The extinct language type
     */
    public static final IRI EXTINCT = FACTORY.createIRI(NAMESPACE, "extinct");

    /**
     * The historical language type
     */
    public static final IRI HISTORICAL = FACTORY.createIRI(NAMESPACE, "historical");

    /**
     * The living language type
     */
    public static final IRI LIVING = FACTORY.createIRI(NAMESPACE, "living");

    /**
     * The unknown language type
     */
    public static final IRI UNKNOWN = FACTORY.createIRI(NAMESPACE, "unknown");


}
