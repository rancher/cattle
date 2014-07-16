<#assign iface = "eth0">
<#assign dummy = "arpproxy">

route flush table 200

<#list ipsecClients as client>
    <#if currentHost.physicalHostId != client.host.physicalHostId && (client.ipAddress.address)?? >
route add ${client.ipAddress.address}/32 dev ${dummy} table 200
    </#if>
</#list>

route flush cache
