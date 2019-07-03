package au.org.ala.vocabulary;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Vocabulary constants for the ALA Format vocabulary.
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @license See LICENSE
 */
public class Format {
    /**
     * The value factory for this vocabulary
     */
    private static final ValueFactory FACTORY = SimpleValueFactory.getInstance();

    /**
     * The Format namespace: http://www.ala.org.au/format/1.0/
     */
    public static final String NAMESPACE = "http://www.ala.org.au/format/1.0/";

    /**
     * The recommended prefix for the Format namespace: "format"
     */
    public static final String PREFIX = "format";

    /**
     * An immutable {@link Namespace} constant that represents the Format namespace.
     */
    public static final Namespace NS = new SimpleNamespace(PREFIX, NAMESPACE);

    /* classes */

    /**
     * An image of some sort
     */
    public static final IRI IMAGE = FACTORY.createIRI(NAMESPACE, "Image");

    /**
     * A taggable concept
     */
    public static final IRI CONCEPT = FACTORY.createIRI(NAMESPACE, "Concept");

    /**
     * A language
     */
    public static final IRI LANGUAGE = FACTORY.createIRI(NAMESPACE, "Language");

    /**
     * A term
     */
    public static final IRI TERM = FACTORY.createIRI(NAMESPACE, "Term");

    /* properties */

    /**
     * The format:asset property
     */
    public static final IRI ASSET = FACTORY.createIRI(NAMESPACE, "asset");

    /**
     * The format:backgroudColor property. A background colour to use during display
     */
    public static final IRI BACKGROUND_COLOR =  FACTORY.createIRI(NAMESPACE, "backgroundColor");

    /**
     * The format:cssClass property. A CSS class to use during display
     */
    public static final IRI CSS_CLASS =  FACTORY.createIRI(NAMESPACE, "cssClass");

    /**
     * The format:height property The width to use during display
     */
    public static final IRI HEIGHT =  FACTORY.createIRI(NAMESPACE, "height");

    /**
     * The format:embed property
     */
    public static final IRI EMBED = FACTORY.createIRI(NAMESPACE, "embed");

    /**
     * The format:icon property. A link to an icon to use for display
     */
    public static final IRI ICON = FACTORY.createIRI(NAMESPACE, "icon");

    /**
     * The format:languageType property
     */
    public static final IRI LANGUAGE_TYPE =  FACTORY.createIRI(NAMESPACE, "languageType");

    /**
     * The format:priority property
     */
    public static final IRI PRIORITY = FACTORY.createIRI(NAMESPACE, "priority");

    /**
     * The format:resolver property
     */
    public static final IRI RESOLVER = FACTORY.createIRI(NAMESPACE, "resolver");

    /**
     * A the format:textColor property. A foreground colour to use during display
     */
    public static final IRI TEXT_COLOR =  FACTORY.createIRI(NAMESPACE, "textColor");

    /**
     * The format:width property The width to use during display
     */
    public static final IRI WIDTH =  FACTORY.createIRI(NAMESPACE, "width");

}

