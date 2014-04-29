<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
        <#if client.domain?? >
            <#assign domain = client.domain>
        <#elseif client.networkDomain?? >
            <#assign domain = client.networkDomain >
        <#else>
            <#assign domain = client.defaultDomain >
        </#if>
0 ${client.macAddress} ${client.ipAddress} ${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"}.${domain} *
    </#if>
</#list>
