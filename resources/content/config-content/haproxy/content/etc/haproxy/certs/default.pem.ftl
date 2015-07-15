<#if defaultCert??>
${defaultCert.key}
${defaultCert.cert}
<#if defaultCert.certChain??> ${defaultCert.certChain}</#if>
</#if>