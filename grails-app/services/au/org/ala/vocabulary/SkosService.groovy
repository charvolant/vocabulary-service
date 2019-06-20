package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.Resource
import grails.gorm.transactions.Transactional
import org.eclipse.rdf4j.model.IRI
import org.eclipse.rdf4j.model.vocabulary.SKOS

class SkosService {
    def repositoryService

    /**
     * Build a tree of a skos heirarchy.
     * <p>
     * The tree always starts from the concept scheme and works downward
     *
     * @param iri A concept scheme or concept
     */
    def tree(String iri) {
        def seen = [] as Set
        def tiri = repositoryService.valueFactory.createIRI(iri)
        def top = repositoryService.getResource(tiri)
        if (!top.hasType(SKOS.CONCEPT_SCHEME)) {
            tiri = top.getStatement(SKOS.IN_SCHEME) as IRI
            if (tiri == null)
                return []
            top = repositoryService.getResource(tiri)
        }
        seen.add(top)
        def members = repositoryService.query("SELECT DISTINCT ?member WHERE { ?member <${SKOS.IN_SCHEME.stringValue()}> <${tiri}>. }").resources
        def high = members.findAll { member ->
            def parents = repositoryService.query("SELECT DISTINCT ?parent WHERE { { <${member.iri.stringValue()}> (<${SKOS.BROADER.stringValue()}>|<${SKOS.BROADER_TRANSITIVE.stringValue()}>) ?parent. } UNION { ?parent (<${SKOS.NARROWER.stringValue()}>|<${SKOS.NARROWER_TRANSITIVE.stringValue()}>) <${member.iri.stringValue()}>. } }").resources
            parents.isEmpty()
        }
        return [resource: top, children: high.collect { hr -> tree2(hr, seen) } ]
    }

    // Recursively collect the broader/narrower tree
    private def tree2(Resource resource, Set<Resource> seen) {
        if (seen.contains(resource))
            return [resource: resource, children: []]
        seen.add(resource)
        def children = children(resource.iri.stringValue())
        return [resource: resource, children: children.collect { child -> tree2(child, seen) }]
    }

    /**
     * Build a list of parent concepts, starting from broadest to narrowest.
     *
     * @param iri The original
     */
    def parents(String iri) {
        def parents = []
        def seen = [] as Set
        def queue = [ repositoryService.valueFactory.createIRI(iri) ]
        while (queue) {
            def nq = []
            def links = []
            queue.each { child ->
                def broader = repositoryService.query("SELECT DISTINCT ?parent WHERE { { <${child.stringValue()}> (<${SKOS.BROADER.stringValue()}>|<${SKOS.BROADER_TRANSITIVE.stringValue()}>) ?parent. } UNION { ?parent (<${SKOS.NARROWER.stringValue()}>|<${SKOS.NARROWER_TRANSITIVE.stringValue()}>) <${child.stringValue()}>. } }")
                broader.resources.each { b ->
                    if (!seen.contains(b.iri)) {
                        nq << b.iri
                        seen << b.iri
                        links << b
                    }
                }
            }
            if (links)
                parents << links
            queue = nq
        }
        return parents.reverse()
    }

    /**
     * Build a list of immediate child concepts.
     *
     * @param iri The original
     */
    def children(String iri) {
        def narrower = repositoryService.query("SELECT DISTINCT ?child WHERE { { ?child (<${SKOS.BROADER.stringValue()}>|<${SKOS.BROADER_TRANSITIVE.stringValue()}>) <${iri}>. } UNION { <${iri}> (<${SKOS.NARROWER.stringValue()}>|<${SKOS.NARROWER_TRANSITIVE.stringValue()}>) ?child. } }")
        return narrower.resources
    }
}
