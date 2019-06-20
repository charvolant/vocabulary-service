package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.AssetResolver
import au.org.ala.vocabulary.model.JSONLDBuilder
import grails.converters.JSON

class ResourceController {
    static responseFormats = ['json' ]

    def resourceService
    def repositoryService

    def types() {
        def offset = params.offset as Integer ?: 0
        def max = params.max as Integer ?: 10
        def locale = request.locale
        def list = resourceService.listTypes(offset, max)
        def builder = new JSONLDBuilder(locale: locale, context: list.context, categorisation: list.categorisation)
        response.contentType = 'application/ld+json;charset=UTF-8'
        render builder.build(list.resources, list.count) as JSON
    }

    def index() {
        def offset = params.offset as Integer ?: 0
        def max = params.max as Integer ?: 10
        def type = params.type ?: ALA.DWC_VOCABULARY.stringValue()
        def locale = request.locale
        def list = resourceService.listResources(type, offset, max)
        def builder = new JSONLDBuilder(locale: locale, context: list.context, categorisation: list.categorisation)
        response.contentType = 'application/ld+json;charset=UTF-8'
        render builder.build(list.resources, list.count) as JSON
    }

    def references() {
        def offset = params.offset as Integer ?: 0
        def max = params.max as Integer ?: 10
        def iri = params.iri
        def locale = request.locale
        def list = resourceService.listReferences(iri, offset, max)
        def builder = new JSONLDBuilder(locale: locale, context: list.context, categorisation: list.categorisation)
        response.contentType = 'application/ld+json;charset=UTF-8'
        render builder.build(list.resources, list.count) as JSON
    }

    def show() {
        def iri = params.iri
        def locale = request.locale
        def rs = resourceService
        def lm = repositoryService.lastModified
        withCacheHeaders {
            etag {
                "${iri}:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def resource = rs.getResource(iri)
                resource.context.resolvers = resolvers()
                def builder = new JSONLDBuilder(locale: locale, context: resource.context, categorisation: resource.categorisation)
                response.contentType = 'application/ld+json;charset=UTF-8'
                render builder.build(resource.resource) as JSON
            }
        }
    }

    def terms() {
        def vs = params.list('v')
        def locale = request.locale
        def vocs = resourceService.listTerms(vs)
        def builder = new JSONLDBuilder(locale: locale, context: vocs.context)
        response.contentType = 'application/ld+json;charset=UTF-8'
        render builder.build(vocs.resources, vocs.count) as JSON
    }

    def search() {
        def q = params.q
        def offset = params.offset as Integer ?: 0
        def max = params.max as Integer ?: 10
        def locale = request.locale
        def results = resourceService.search(q, offset, max)
        def builder = new JSONLDBuilder(locale: locale, context: results.context)
        response.contentType = 'application/ld+json;charset=UTF-8'
        render builder.build(results.resources, results.count) as JSON
    }

    /**
     * Special case resolvers for URLs
     */
    private def resolvers() {
        return [
                (Format.ASSET): new AssetResolver(factory: repositoryService.valueFactory, path: { s -> assetPath(src: s, absolute: true) })
        ]
    }
}
