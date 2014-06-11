<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
${client.macAddress},${client.ipAddress},set:host${client.instanceId},${client.fqdn},infinite
    </#if>
</#list>
