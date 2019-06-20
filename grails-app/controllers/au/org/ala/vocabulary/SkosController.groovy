package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.Resource
import grails.converters.JSON

/**
 * Provide information for SKOS heirarchies.
 */
class SkosController {
    def repositoryService
    def skosService
    def resourceService

    /**
     * Get a complete SKOS tree
     */
    def tree() {
        def iri = params.iri
        def locale = request.locale
        def ss = skosService
        def lm = repositoryService.lastModified
        withCacheHeaders {
            etag {
                "${iri}:tree:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def tree = ss.tree(iri)
                def result = walkTree(tree)
                response.contentType = 'application/json;charset=UTF-8'
                render result as JSON
            }
        }
    }

    /**
     * Get the chain of parent concepts for
     * @return
     */
    def parents() {
        def iri = params.iri
        def locale = request.locale
        def ss = skosService
        def lm = repositoryService.lastModified
        withCacheHeaders {
            etag {
                "${iri}:parents:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def parents = ss.parents(iri)
                def result = walkTree(parents)
                response.contentType = 'application/json;charset=UTF-8'
                render result as JSON
            }
        }
    }
    /**
     * Get the immediate children of a concept
     * @return
     */
    def children() {
        def iri = params.iri
        def locale = request.locale
        def ss = skosService
        def lm = repositoryService.lastModified
        withCacheHeaders {
            etag {
                "${iri}:children:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def children = ss.children(iri)
                def result = walkTree(children)
                response.contentType = 'application/json;charset=UTF-8'
                render result as JSON
            }
        }
    }

    private def resourceConverter = { Resource resource ->
        [
                iri: resource.iri.stringValue(),
                label: resourceService.getLabel(resource, request.locale)
        ]
    }

    private def walkTree(source, Closure converter = resourceConverter) {
        if (source in Resource)
            return converter.call(source)
        if (source in Map)
            return source.collectEntries {k, v  -> [(k): walkTree(v, converter) ]}
        if (source in Collection)
            return source.collect { sub -> walkTree(sub, converter) }
        return source
    }
}
