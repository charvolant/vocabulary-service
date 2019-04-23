package au.org.ala.vocabulary.model


import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.vocabulary.*

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
    static SIMPLE_PREDICATES = [ SKOS.PREF_LABEL, RDFS.LABEL, DCTERMS.TITLE, DC.TITLE, RDFS.COMMENT, DCTERMS.DESCRIPTION, DC.DESCRIPTION ] as Set

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
        jsonld.putAll(this.render(resource, true, false, null))
        jsonld['@context'] = this.render(context, true, true, null)
        if (categorisation)
            jsonld['@categorisation'] = this.render(categorisation, true,null)
        return jsonld
    }

    def build(List<Resource> list, Integer count = null) {
        def jsonld = new LinkedHashMap() // Ensure order of results
        jsonld['@language'] = locale.language
        if (count != null) // Allow a count of 0
            jsonld['@count'] = count
        jsonld['@graph'] = list.collect { this.render(it, true, false, null) }
        jsonld['@context'] = this.render(context, true, true, null)
        if (categorisation)
            jsonld['@categorisation'] = this.render(categorisation, true, null)
        return jsonld
    }

    def render(Context context, boolean expand, boolean simple, Resolver resolver) {
        def jsonld = new LinkedHashMap()
        jsonld.putAll((context.namespaces.values() as Set).collectEntries { [(it.prefix): it.name] })
        jsonld.putAll(context.resources.collectEntries { [(context.shortLabel(it.key)): render(it.value, expand, simple, resolver)]})
        return jsonld
    }

    def render(Categorisation categorisation, boolean simple, Resolver resolver) {
        def jsonld = new LinkedHashMap()
        categorisation.categories.each { category ->
            def predicates = categorisation.categorisation[category]
            jsonld[context.shortLabel(category)] = predicates.collect { predicate -> context.getReference(predicate)}
        }
        return jsonld
    }

    def render(Resource resource, boolean expand, boolean simple, Resolver resolver) {
        def jsonld = new LinkedHashMap() // Ensure order of results
        jsonld['@id'] = resource.iri.stringValue()
        jsonld['@type'] = render(resource.getStatements(RDF.TYPE), false, null) ?: '@id'
        if (expand) {
            def predicates = new ArrayList<IRI>(resource.predicates)
            predicates = predicates.sort(true, { p1, p2 -> context.shortLabel(p1).compareTo(context.shortLabel(p2)) })
            predicates.each { p ->
                def r = context.resolver(p) ?: resolver
                def s = (simple && SIMPLE_PREDICATES.contains(p)) ? resource.findStatement(locale, p) : resource.getStatements(p)
                jsonld[context.getReference(p)] = render(s, simple, r)
            }
        } else {
        }
        return jsonld
    }

    def render(List<Value> values, boolean simple, Resolver resolver) {
        if (!values)
            return null
        if (values.size() == 1)
            return render(values.first(), simple, resolver)
        values.sort(new ValueComparator())
        return values.collect { render(it, simple, resolver) }
    }

    def render(IRI iri, boolean simple, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(iri), simple, null)
        return context.getReference(iri)
    }

    def render(Literal literal, boolean simple, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(literal), simple, null)
        def jsonld = new LinkedHashMap()
        def datatype = literal.datatype ?: XMLSchema.STRING
        def value = literal.label
        switch (datatype) {
            case XMLSchema.BOOLEAN:
                value = literal.booleanValue()
                break
            case XMLSchema.DATE:
                def calendar = literal.calendarValue()
                if (calendar)
                    value = DATE_FORMAT.format(calendar.toGregorianCalendar().time)
                break
            case XMLSchema.DATETIME:
                def calendar = literal.calendarValue()
                if (calendar)
                    value = DATE_TIME_FORMAT.format(literal.calendarValue().toGregorianCalendar().time)
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
            jsonld['@type'] = context.getReference(datatype)
        if (literal.language.present)
            jsonld['@language'] = literal.language.get()
        return jsonld
    }

    def render(Value value, boolean simple, Resolver resolver) {
        if (resolver != null)
            return render(resolver.resolve(value), simple, null)
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
