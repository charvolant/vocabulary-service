package au.org.ala.vocabulary.model

import au.org.ala.vocabulary.Format
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.vocabulary.DC
import org.eclipse.rdf4j.model.vocabulary.DCTERMS
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.model.vocabulary.RDFS
import org.eclipse.rdf4j.model.vocabulary.SKOS
import org.eclipse.rdf4j.model.vocabulary.XMLSchema

import java.text.SimpleDateFormat

/**
 * Build a JSON-LD model of a set of
 *
 * @author Doug Palmer &lt;Doug.Palmer@csiro.au&gt;
 * @copyright Copyright &copy; 2018 Atlas of Living Australia
 */
class JSONLDBuilder {
    static DATE_FORMAT = new SimpleDateFormat('yyyy-MM-dd')
    static DATE_TIME_FORMAT = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX')

    /** The preferred locale */
    Locale locale
    /** The context */
    Context context
    /** The categorisation */
    Categorisation categorisation

    def build(Resource resource) {
        def jsonld = new LinkedHashMap() // Ensure order of results
        jsonld['@id'] = resource.iri.stringValue()
        jsonld['@language'] = locale.language
        jsonld.putAll(this.render(resource, true, null))
        jsonld['@context'] = this.render(context, true, null)
        if (categorisation)
            jsonld['@categorisation'] = this.render(categorisation, null)
        return jsonld
    }

    def build(List<Resource> list, Integer count = null) {
        def jsonld = new LinkedHashMap() // Ensure order of results
        jsonld['@language'] = locale.language
        if (count != null) // Allow a count of 0
            jsonld['@count'] = count
        jsonld['@graph'] = list.collect { this.render(it, true, null) }
        jsonld['@context'] = this.render(context, true, null)
        if (categorisation)
            jsonld['@categorisation'] = this.render(categorisation, null)
        return jsonld
    }

    def render(Context context, boolean expand, Resolver resolver) {
        def jsonld = new LinkedHashMap()
        jsonld.putAll((context.namespaces.values() as Set).collectEntries { [(it.prefix): it.name] })
        jsonld.putAll(context.resources.collectEntries { [(context.shortLabel(it.key)): render(it.value, expand, resolver)]})
        return jsonld
    }

    def render(Categorisation categorisation, Resolver resolver) {
        def jsonld = new LinkedHashMap()
        categorisation.categories.each { category ->
            def predicates = categorisation.categorisation[category]
            jsonld[context.shortLabel(category)] = predicates.collect { predicate -> context.shortLabel(predicate)}
        }
        return jsonld
    }

    def render(Resource resource, boolean expand, Resolver resolver) {
        def jsonld = new LinkedHashMap() // Ensure order of results
        jsonld['@id'] = resource.iri.stringValue()
        jsonld['@type'] = render(resource.getStatements(RDF.TYPE), null) ?: '@id'
        def label = resource.findTerm(locale, SKOS.PREF_LABEL, RDFS.LABEL, SKOS.NOTATION)
        jsonld['@label'] = label ?: resource.iri.localName
        jsonld['@shortId'] = context.shortLabel(resource.iri)
        def title = resource.findTerm(locale, DCTERMS.TITLE, DC.TITLE)
        if (title) {
            if (!label && title.length() < 32) {
                jsonld['@label'] = title
            }
            jsonld['@title'] = title
        }
        def description = resource.findTerm(locale, DCTERMS.DESCRIPTION, DC.DESCRIPTION, RDFS.COMMENT)
        if (description)
            jsonld['@description'] = description
        if (expand) {
            def predicates = new ArrayList<IRI>(resource.predicates)
            predicates = predicates.sort(true, { p1, p2 -> context.shortLabel(p1).compareTo(context.shortLabel(p2)) })
            predicates.each { p ->
                def r = context.resolver(p) ?: resolver
                jsonld[context.shortLabel(p)] = render(resource.getStatements(p), r)
            }
        } else {
        }
        return jsonld
    }

    def render(List<Value> values, Resolver resolver) {
        if (!values)
            return null
        if (values.size() == 1)
            return render(values.first(), resolver)
        values.sort(new ValueComparator())
        return values.collect { render(it, resolver) }
    }

    def render(IRI iri, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(iri), null)
        return context.shortLabel(iri)
    }

    def render(Literal literal, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(literal), null)
        def jsonld = new LinkedHashMap()
        def datatype = literal.datatype ?: XMLSchema.STRING
        def value = literal.label
        switch (datatype) {
            case XMLSchema.BOOLEAN:
                value = literal.booleanValue()
                break
            case XMLSchema.DATE:
                value = DATE_FORMAT.format(literal.calendarValue())
                break
            case XMLSchema.DATETIME:
                value = DATE_TIME_FORMAT.format(literal.calendarValue())
                break
            case XMLSchema.DECIMAL:
                value = literal.decimalValue()
                break
            case XMLSchema.DOUBLE:
            case XMLSchema.FLOAT:
                value = literal.doubleValue()
                break
            case XMLSchema.BYTE:
            case XMLSchema.INT:
            case XMLSchema.INTEGER:
            case XMLSchema.LONG:
            case XMLSchema.SHORT:
                value = literal.intValue()
                break
            case RDF.LANGSTRING:
                datatype = XMLSchema.STRING
                break
        }
        jsonld['@value'] = value
        if (datatype != XMLSchema.STRING)
            jsonld['@type'] = datatype.stringValue()
        if (literal.language.present)
            jsonld['@language'] = literal.language.get()
        return jsonld
    }

    def render(Value value, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(value), null)
        return value.stringValue()
    }

    /** Order objects by type and language. Slide the locale language to the top, if possible */
    class ValueComparator implements Comparator<Object> {
        private int order(Object o) {
            if (o in Resource || o in IRI)
                return 0
            if (o in Literal)
                return 1
            return 2
        }

        private int lang(Literal literal) {
            if (literal.language.present && literal.language.get() == locale.language)
                return 0
            if (!literal.language.present)
                return 1
            return 2
        }

        @Override
        int compare(Object o1, Object o2) {
            int p1 = order(o1)
            int p2 = order(o2)
            if (p1 != p2)
                return p1 - p2
            if (o1 in Literal && o2 in Literal) {
                def l1 = lang(o1)
                def l2 = lang(o2)
                if (l1 != l2) {
                    return l1 - l2
                }
                if (o1.language.present && o2.language.present)
                    return o1.language.get().compareTo(o2.language.get())
            }
            if (o1 in Resource)
                o1 = o1.iri
            if (o2 in Resource)
                o2 = o2.iri
            if (o1 in Value && o2 in Value)
                return o1.stringValue().compareTo(o2.stringValue())
            return o1.toString().compareTo(o2.toString())
        }
    }

}
