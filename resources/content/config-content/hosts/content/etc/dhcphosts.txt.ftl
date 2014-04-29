<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
        <#if client.domain?? >
            <#assign domain = client.domain>
        <#elseif client.networkDomain?? >
            <#assign domain = client.networkDomain >
        <#else>
            <#assign domain = client.defaultDomain >
        </#if>
${client.macAddress},${client.ipAddress},set:host${client.instanceId},${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"}.${domain},infinite
    </#if>
</#list>
