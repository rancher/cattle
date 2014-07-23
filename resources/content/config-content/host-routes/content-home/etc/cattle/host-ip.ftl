<#if ipAssociations?? >
    <#list ipAssociations as assoc >
default ${assoc.ipAddress.address} ${assoc.targetIpAddress.address}
    </#list>
</#if>
