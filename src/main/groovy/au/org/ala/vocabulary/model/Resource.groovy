package au.org.ala.vocabulary.model

import org.eclipse.rdf4j.common.iteration.Iteration
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Statement
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.vocabulary.DC
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.SKOS
import org.eclipse.rdf4j.model.vocabulary.XMLSchema

/**
 * Information about a resource.
 * <p>
 * Resources are usually cached and not mutable.
 * To make a mutable copy, use {@link #clone()}
 * </p>
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
class Resource implements Cloneable {
    IRI iri
    /** Values of the resource (possibly recursively */
    List<Statement> statements
    /** The known predicates */
    Set<IRI> predicates
    /** Indicate a mutable version. */
    boolean mutable

    /**
     * Create a resource description
     *
     * @param iri The resource IRI
     * @param statements The statements that apply to this IRII
     */
    Resource(IRI iri, Iteration<Statement> statements) {
        this.iri = iri
        this.statements = new ArrayList<>()
        while (statements.hasNext()) {
            this.statements << statements.next()
        }
        this.predicates = this.statements.collect({ it.predicate }) as Set
        this.mutable = false
    }

    /**
     * Make a mutable copy of this resource, to allow modifications
     *
     * @return The mutable copy
     */
    @Override
    public Resource clone()  {
        def clone = super.clone() as Resource
        clone.statements = new ArrayList<Statement>(clone.statements)
        clone.predicates = new HashSet<IRI>(clone.predicates)
        clone.mutable = true
        return clone
    }

    /**
     * Add a statement to a mutable resource.
     *
     * @param statement The additional statement
     */
    void addStatement(Statement statement) {
        if (!mutable)
            throw new IllegalStateException("Unmutable resource")
        statements.add(statement)
        predicates.add(statement.predicate)
    }

    /**
     * Does this resource have a specific type.
     *
     * @param type The type
     *
     * @return True if the type is listed
     */
    boolean hasType(IRI type) {
        return this.statements.any { s -> s.predicate == RDF.TYPE && s.object == type }
    }

    /**
     * Test to see if we have a boolean value
     *
     * @param predicate The predicate
     * @param value The value to test against
     *
     * @return True if there is a predicate that matches the value
     */
    boolean hasValue(IRI predicate, boolean value) {
        return this.statements.any { s ->
            s.predicate == predicate &&
                    s.object in Literal &&
                    ((Literal) s.object).datatype == XMLSchema.BOOLEAN &&
                    s.object.stringValue().asBoolean() == value
        }
    }

    /**
     * Get the list of values associated with a predicate
     *
     * @param predicates The possible predicates
     *
     * @return The found statements
     */
    List<Value> getStatements(IRI... predicates) {
        return statements.findAll({ s -> predicates.contains(s.predicate) }).collect({ it.object })
    }

    /**
     * Get the first value that matches a predicate
     *
     * @param predicates The predicates
     *
     * @return The found value
     */
    Value getStatement(IRI... predicates) {
        for (IRI p: predicates) {
            Statement st = statements.find { s -> s.predicate == p }
            if (st)
                return st.object
        }
    }

    /**
     * Find a locale-preferred term for something.
     * <p>
     * Possible terms are first searched for a language/country tag that matches, then a language tag, and then any value
     *
     * @param locale The preferred locale
     * @param sources Possible sources of the term
     *
     * @return The result
     */
    String findTerm(Locale locale, IRI... sources) {
        def tag = locale?.toLanguageTag()
        def lang = locale?.language
        for (IRI source: sources) {
            def match
            if (tag) {
                match = statements.find {
                    (it.predicate == source) && (it.object instanceof Literal) && it.object.language?.present && it.object.language.get() == tag
                }
                if (match)
                    return match.object.stringValue()
            }
            if (lang) {
                match = statements.find {
                    (it.predicate == source) && (it.object instanceof Literal) && it.object.language?.present && it.object.language.get() == lang
                }
                if (match)
                    return match.object.stringValue()
            }
            match = statements.find { (it.predicate == source) && (it.object instanceof Literal) }
            if (match)
                return match.object.stringValue()
        }
        return null
    }

    /**
     * Find all the locale-preferred terms for something.
     * <p>
     * Possible terms are first searched for a language/country tag that matches, then a language tag, and then any value
     *
     * @param locale The preferred locale
     * @param sources Possible sources of the term
     *
     * @return The result
     */
    List<String> findTerms(Locale locale, IRI... sources) {
        def tag = locale?.toLanguageTag()
        def lang = locale?.language
        def terms = []
        for (IRI source: sources) {
            def matches
            if (tag) {
                matches = statements.findAll {
                    (it.predicate == source) && (it.object instanceof Literal) && it.object.language?.present && it.object.language.get() == tag
                }
            }
            if (!matches && lang) {
                matches = statements.findAll {
                    (it.predicate == source) && (it.object instanceof Literal) && it.object.language?.present && it.object.language.get() == lang
                }
            }
            if (!matches)
                matches = statements.findAll { (it.predicate == source) && (it.object instanceof Literal) }
            terms = matches.inject(terms, { ts, st -> ts << st.object.stringValue(); ts})
        }
        return terms
    }

}
