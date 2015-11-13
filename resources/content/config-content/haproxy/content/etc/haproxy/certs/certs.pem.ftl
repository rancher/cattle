<#if certs?? && certs?has_content>
<#list certs as cert >
${cert.key}
${cert.cert}
<#if cert.certChain??>${cert.certChain}</#if>
</#list>
</#if>
