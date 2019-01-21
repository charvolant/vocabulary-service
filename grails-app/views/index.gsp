<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="${grailsApplication.config.skin.layout}"/>
    <title>Vocabulary Web services | ${grailsApplication.config.skin.orgNameLong}</title>
    <asset:javascript src="swagger-ui.js"/>
    <asset:stylesheet src="swagger-ui.css"/>
</head>
<body>
<div>
    <auth:ifAllGranted roles="ROLE_ADMIN">
        <g:link controller="admin" action="index" class="btn bth-warning"><g:message code="page.admin.index.menu"/></g:link>
    </auth:ifAllGranted>
</div>
<div role="main" id="swagger-ui">
</div>
<asset:script type="application/javascript">
    window.onload = function() {
    var ui = SwaggerUIBundle({
        url: "${resource(file: 'openapi.yml')}",
        dom_id: '#swagger-ui',
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ]
    })
    window.ui = ui
}
</asset:script>
</body>
</html>
