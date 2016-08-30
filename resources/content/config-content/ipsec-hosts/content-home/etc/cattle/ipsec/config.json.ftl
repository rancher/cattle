{
"entries": [
<#list ipsecClients as client>
    <#if (client.ipAddress.address)?? && client.host.physicalHostId == currentHost.physicalHostId && client.instance.id == client.agentInstance.id >
        <#assign agentInstanceIp = client.ipAddress.address >
    </#if>
</#list>

<#if agentInstanceIp?? >
    <#list ipsecClients as client>
        <#if (client.ipAddress.address)?? && (client.hostIpAddress.address)?? >
{
            <#if client.instance.id == client.agentInstance.id >
    "peer": true,
            </#if>
            <#if client.host.physicalHostId == currentHost.physicalHostId && client.instance.id == client.agentInstance.id >
    "self": true,
            </#if>
    "ip": "${client.ipAddress.address}/${client.subnet.cidrSize}",
    "hostIp": "${client.hostIpAddress.address}"
},
        </#if>
    </#list>
</#if>
{}]
}