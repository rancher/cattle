<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
0 ${client.macAddress} ${client.ipAddress} ${client.fqdn} *
    </#if>
</#list>
