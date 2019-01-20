package au.org.ala.vocabulary

import grails.converters.JSON

class AdminController {
    static responseFormats = ['json' ]

    def repositoryService
    def categorisationService
    def tagService

    /**
     * Clear caches after a change
     */
    def clearCaches() {
        repositoryService.clearCaches()
        categorisationService.clearCaches()
        tagService.clearCaches()
        render ([success: true] as JSON)
    }
}
