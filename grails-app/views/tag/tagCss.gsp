<g:each var="tag" status="ss" in="${tags}">
.${tag.tagClass} {
    <g:if test="${tag.backgroundColor}">background-color: ${tag.backgroundColor};</g:if>
    <g:if test="${tag.textColor}">color: ${tag.textColor};</g:if>
    <g:if test="${tag.icon}">background: url(${tag.icon}) no-repeat center center;</g:if>
    <g:if test="${tag.width}">width: ${tag.width}px;</g:if>
    <g:if test="${tag.height}">height: ${tag.height}px;</g:if>
}
<g:if test="${tag.icon}">
.${tag.tagClass} span {
    display: none;
}
</g:if>
</g:each>
