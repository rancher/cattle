<#assign iface = "eth0">
<#assign dummy = "lo">

route flush table 200

<#list ipsecClients as client>
    <#if currentHost.id != client.host.id >
route add ${client.ipAddress.address}/32 dev ${dummy} table 200
    </#if>
</#list>

route flush cache
