/* Tag library information */

var VOCABULARY_TAG_DATA = [
<g:each var="tag" status="ts" in="${tags}">
    {
        key: <fu:jsString value="${tag.key}"/>,
        vocabulary: <fu:jsString value="${tag.vocabulary}"/>,
        id: <fu:jsString value="${tag.id}"/>,
        label: <fu:jsString value="${tag.label}"/>,
        title: <fu:jsString value="${tag.title}"/>,
        description: <fu:jsString value="${tag.description}"/>,
        css: <fu:jsString value="${tag.cssClass.join(' ')}"/>
    }<g:if test="${ts < tags.size() - 1}">,</g:if>
</g:each>
];

var VOCABULARY_LOOKUP_ID = {
<g:each var="tag" status="ts" in="${tags}">
    <fu:jsString value="${tag.id}"/>: VOCABULARY_TAG_DATA[${indexes[tag.id]}]<g:if test="${ts < tags.size() - 1}">,</g:if>
</g:each>
};

var VOCABULARY_LOOKUP_VOC = {
<g:each var="voc" status="vs" in="${vocabularies}">
    '${voc.encodeAsJavaScript()}': {
    <g:set var="vtags" value="${tags.findAll({ it.vocabulary == voc })}"/>
    <g:each var="tag" status="ts" in="${vtags}">
        <fu:jsString value="${tag.key}"/>: VOCABULARY_TAG_DATA[${indexes[tag.id]}]<g:if test="${ts < vtags.size() - 1}">,</g:if>
    </g:each>
    }<g:if test="${vs < vocabularies.size() - 1}">,</g:if>
</g:each>
};

var VOCABULARY_LOOKUP_TERM = {
<g:each var="term" status="ts" in="${terms.keySet()}">
    <g:set var="matches" value="${terms[term]}"/><fu:jsString value="${term}"/>: <g:if test="${matches && matches.size() == 1}">VOCABULARY_TAG_DATA[${indexes[matches[0].id]}]</g:if><g:else>null</g:else><g:if test="${ts < tags.size() - 1}">,</g:if>
</g:each>
};
