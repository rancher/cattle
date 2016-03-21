<#if serviceSet?seq_contains("dnsService") >
    <#list services["dnsService"].nicNames as nic >
        <#list vnetClients as vnetClient >
            <#if (vnetClient.ipAddress)?? && (vnetClient.macAddress)?? >
${vnetClient.ipAddress} ${vnetClient.macAddress} temp
            </#if>
        </#list>
    </#list>
</#if>