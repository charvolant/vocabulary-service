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

    /**
     * The ala:TaxonRank class
     */
    public static final IRI TAXON_RANK = FACTORY.createIRI(NAMESPACE, "TaxonRank");

    /* properties */

    /**
     * The name used to label a term in CSV
     */
    public static final IRI CSV_NAME = FACTORY.createIRI(NAMESPACE, "csvName");

    /**
     * The ala:DwCVocabulary class
     */
    public static final IRI FOR_TERM = FACTORY.createIRI(NAMESPACE, "forTerm");

    /**
     * The name used to label a term in JSON
     */
    public static final IRI JSON_NAME= FACTORY.createIRI(NAMESPACE, "jsonName");

    /**
     * The name used to label a term in a SOLR index
     */
    public static final IRI SOLR_NAME = FACTORY.createIRI(NAMESPACE, "solrTerm");

    /**
     * The status of an object
     */
    public static final IRI STATUS = FACTORY.createIRI(NAMESPACE, "status");

    /**
     * Is this a linnaean concept?
     */
    public static final IRI IS_LINNAEAN_RANK = FACTORY.createIRI(NAMESPACE, "isLinnaeanRank");

    /**
     * Is this a loose concept?
     */
    public static final IRI IS_LOOSE_RANK = FACTORY.createIRI(NAMESPACE, "isLooseRank");

    /**
     * The rank identifier
     */
    public static final IRI RANK_ID = FACTORY.createIRI(NAMESPACE, "rankID");

    /**
     * The rank marker (used in scientific names)
     */
    public static final IRI RANK_MARKER = FACTORY.createIRI(NAMESPACE, "rankMarker");

    /**
     * The sort order of an object
     */
    public static final IRI SORT_ORDER = FACTORY.createIRI(NAMESPACE, "sortOrder");

}

