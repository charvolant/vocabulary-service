<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title><g:message code-="page.language.index.title"/> | ${grailsApplication.config.skin.orgNameLong}</title>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1><g:message code="page.admin.index.title"/></h1>
    <g:if test="${flash.errorMessage}"><div class="row bg-danger"><div class="col-md-12"><p>${flash.errorMessage}</p></div></div></g:if>
    <g:if test="${flash.message}"><div class="row bg-info" ><div class="col-md-12"><p>${flash.message}</p></div></div></g:if>
    </div>
    <div class="row">
        <div class="col-md-12">
            <h2><g:message code="label.admin"/></h2>
            <g:link action="clearCaches" class="btn btn-primary"><g:message code="label.clearCaches"/></g:link> <g:message code="label.clearCaches.detail"/>
            <g:link controller="alaAdmin" action="index" class="btn btn-primary"><g:message code="label.adminInfo"/></g:link> <g:message code="label.adminInfo.detail"/>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <h2><g:message code="label.iso-639-3-upload"/></h2>
            <g:message code="label.iso-639-3-upload.detail"/>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <g:uploadForm action="process639">
                <g:hiddenField name="translation" value="iso639"/>
                <div class="form-group">
                    <label for="nameFile"><g:message code="label.nameFile"/></label>
                    <span class="text-danger"><g:fieldError field="nameFile" bean="${cmd}"/></span>
                    <g:field id="nameFile" name="nameFile" type="file" required="true" class="form-control" value="${cmd.nameFile}"/>
                    <p class="help-block"><g:message code="label.nameFile.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="macroFile"><g:message code="label.macroFile"/></label>
                    <span class="text-danger"><g:fieldError field="macroFile" bean="${cmd}"/></span>
                    <g:field id="macroFile" name="macroFile" type="file" required="true" class="form-control" value="${cmd.macroFile}"/>
                    <p class="help-block"><g:message code="label.macroFile.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="tagLanguages"><g:message code="label.tagLanguages"/></label>
                    <span class="text-danger"><g:fieldError field="tagLanguages" bean="${cmd}"/></span>
                    <g:field id="tagLanguages" name="tagLanguages" type="text" class="form-control" value="${cmd.tagLanguages}"/>
                    <p class="help-block"><g:message code="label.tagLanguages.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="complete-639" class="form-check-label">
                        <g:checkBox id="complete-639" name="complete" value="${cmd.complete}"/>
                        <g:message code="label.complete"/>
                        <g:fieldError field="complete" bean="${cmd}"/>
                    </label>
                    <p class="help-block"><g:message code="label.complete.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="format-639"><g:message code="label.format"/></label>
                    <span class="text-danger"><g:fieldError field="format" bean="${cmd}"/></span>
                    <g:select id="format-639" name="format" from="${['text/turtle', 'application/ld+json', 'application/rdf+xml']}" value="${cmd.format}"/>
                    <p class="help-block"><g:message code="label.format.detail"/></p>
                </div>
                <button type="submit" class="btn btn-primary"><g:message code="label.process"/></button>
            </g:uploadForm>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <h2><g:message code="label.dwc-upload"/></h2>
            <g:message code="label.dwc-upload.detail"/>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12">
            <g:uploadForm action="processDwC">
                <g:hiddenField name="translation" value="dwc"/>
                <div class="form-group">
                    <label for="dwcFile"><g:message code="label.dwcFile"/></label>
                    <span class="text-danger"><g:fieldError field="dwcFile" bean="${cmd}"/></span>
                    <g:field id="dwcFile" name="dwcFile" type="file" required="true" class="form-control"/>
                    <p class="help-block"><g:message code="label.dwcFile.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="biocacheIndexFields"><g:message code="label.biocacheIndexFields"/></label>
                    <span class="text-danger"><g:fieldError field="biocacheIndexFields" bean="${cmd}"/></span>
                    <g:field id="biocacheIndexFields" name="biocacheIndexFields" type="url" required="false" class="form-control" value="${cmd.biocacheIndexFields}"/>
                    <p class="help-block"><g:message code="label.biocacheIndexFields.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="complete-dwc" class="form-check-label">
                        <g:checkBox id="complete-dwc" name="complete" value="${cmd.complete}"/>
                        <g:message code="label.complete"/>
                        <g:fieldError field="complete" bean="${cmd}"/>
                    </label>
                    <p class="help-block"><g:message code="label.complete.detail"/></p>
                </div>
                <div class="form-group">
                    <label for="format-dwc" class><g:message code="label.format"/></label>
                    <span class="text-danger"><g:fieldError field="format" bean="${cmd}"/></span>
                    <g:select id="format-dwc" name="format" from="${['text/turtle', 'application/ld+json', 'application/rdf+xml']}" value="${cmd.format}"/>
                    <p class="help-block"><g:message code="label.format.detail"/></p>
                </div>
                 <button type="submit" class="btn btn-primary"><g:message code="label.process"/></button>
            </g:uploadForm>
        </div>
    </div>
</div>
</body>
</html>