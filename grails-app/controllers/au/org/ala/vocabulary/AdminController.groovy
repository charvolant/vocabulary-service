package au.org.ala.vocabulary

import au.org.ala.web.AlaSecured
import au.org.ala.web.CASRoles
import grails.converters.JSON
import grails.validation.Validateable
import org.eclipse.rdf4j.model.Model
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.springframework.web.multipart.MultipartFile

class AdminController {
    def repositoryService
    def categorisationService
    def tagService
    def translationService

    /**
     * Admin index
     */
    def index() {
        def cmd = new TranslateCommand()
        cmd.format = RDFFormat.TURTLE.defaultMIMEType
        cmd.tagLanguages = grailsApplication.config.languages.tag.default
        cmd.biocacheIndexFields = new URL(grailsApplication.config.import.biocache.index.fields)
        render view: 'index', model: [cmd: cmd]
    }

    /**
     * Clear caches after a change
     */
    @AlaSecured(CASRoles.ROLE_ADMIN)
    def clearCaches() {
        repositoryService.clearCaches()
        categorisationService.clearCaches()
        tagService.clearCaches()
        flash.message = message(code: 'message.clearCaches')
        withFormat {
            html {
                redirect action: 'index'
            }
            '*' {
                render ([success: true] as JSON)
            }
        }
    }

    /**
     * Process ISO-639-3 data from https://iso639-3.sil.org into RDF
     */
    def process639(TranslateCommand cmd) {
        if (!cmd || cmd.hasErrors()) {
            render view: 'index', model: [cmd: cmd]
            return
        }
        Model model = translationService.process639(cmd.nameFile, cmd.macroFile, cmd.tagLanguages, request.locale, cmd.complete)
        RDFFormat fmt = RDFFormat.matchMIMEType(cmd.format, [RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.RDFXML]).orElse(RDFFormat.TURTLE)
        response.characterEncoding = "UTF-8"
        //response.setHeader('Content-Disposition', "Attachment;Filename=\"iso639.${fmt.defaultFileExtension}\"")
        response.setHeader('Content-Disposition', "inline")
        response.contentType = fmt.defaultMIMEType
        Rio.write(model, response.outputStream, fmt)
    }

    def processDwC(TranslateCommand cmd) {
        if (!cmd || cmd.hasErrors()) {
            render view: 'index', model: [cmd: cmd]
            return
        }
        Model model = translationService.processDwC(cmd.dwcFile, cmd.biocacheIndexFields, request.locale, cmd.complete)
        RDFFormat fmt = RDFFormat.matchMIMEType(cmd.format, [RDFFormat.TURTLE, RDFFormat.JSONLD, RDFFormat.RDFXML]).orElse(RDFFormat.TURTLE)
        response.characterEncoding = "UTF-8"
        //response.setHeader('Content-Disposition', "Attachment;Filename=\"dwc.${fmt.defaultFileExtension}\"")
        response.setHeader('Content-Disposition', "inline")
        response.contentType = fmt.defaultMIMEType
        Rio.write(model, response.outputStream, fmt)
    }


}

class TranslateCommand implements Validateable {
    /** The type of translation */
    String translation
    /** The TSV file containing translation codes and names */
    MultipartFile nameFile
    /** The TSV file containing macrolangiage definitions */
    MultipartFile macroFile
    /** The CSV file containing darwin core terms */
    MultipartFile dwcFile
    /** The biocache index fields */
    URL biocacheIndexFields
    /** The list of taggable translation codes */
    String tagLanguages
    /** Include all languages */
    boolean complete
    /** The output format */
    String format

    static constraints = {
        def fileValidator = { trans, val, obj ->
            if (val) {
                if (!['tab', 'tsv', 'txt', 'csv'].any { extension -> val.originalFilename?.toLowerCase()?.endsWith(extension) } )
                    return ['format']
            } else if (trans == obj.translation) {
                return ['missing']
            }
        }
        nameFile nullable: true, validator: fileValidator.curry('iso639')
        macroFile nullable: true, validator: fileValidator.curry('iso639')
        dwcFile nullable: true, validator: fileValidator.curry('dwc')
        tagLanguages nullable: true, matches: '[a-z]{3}?(,[a-z]{3})*'
        biocacheIndexFields nullable: true, url: true
    }
}

