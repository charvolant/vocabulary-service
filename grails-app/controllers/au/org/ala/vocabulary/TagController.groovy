package au.org.ala.vocabulary

import au.org.ala.vocabulary.model.AssetResolver

class TagController {
    def tagService
    def repositoryService

    /**
     * Get CSS describing how to render all tags.
     */
    def css() {
        def locale = request.locale
        def lm = repositoryService.lastModified
        def ts = tagService
        withCacheHeaders {
            etag {
                "tag-css:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def resolve = resolvers()
                def tags = ts.tags(locale, resolve)
                render(view: 'tagCss', model: [tags: tags], contentType: 'text/css', encoding: 'UTF-8')
            }
        }
    }

    /**
     * Get javascript describing each tag
     * <p>
     * Locale dependent, so this is not shared-cachable
     * </p>
     */
    def js() {
        def locale = request.locale
        def lm = repositoryService.lastModified
        def ts = tagService
        withCacheHeaders {
            etag {
                "tag-js:${locale.toLanguageTag()}"
            }
            delegate.lastModified {
                lm
            }
            generate {
                def resolve = resolvers()
                def tags = ts.tags(locale, resolve)
                def indexes = tags.inject([:]) { indexes, tag ->
                    indexes[tag.id] = indexes.size()
                    indexes
                }
                def vocabularies = (tags.collect { it.vocabulary }) as Set
                def terms = tags.inject([:]) { terms, tag ->
                    def p = terms[tag.key]
                    if (!p) {
                        p = []
                        terms[tag.key] = p
                    }
                    p << tag
                    terms
                }
                render(view: 'tagJs', model: [tags: tags, indexes: indexes, vocabularies: vocabularies, terms: terms], contentType: 'application/javascript', encoding: 'UTF-8')
            }
        }
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
