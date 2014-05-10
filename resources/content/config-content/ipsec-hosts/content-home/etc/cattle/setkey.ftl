#!/usr/sbin/setkey -f

spdflush;

<#list ipsecClients as client>
    <#if client.host.id == currentHost.id && client.instance.id == client.agentInstance.id >
        <#assign agentInstanceIp = client.ipAddress.address >
    </#if>
</#list>

<#if agentInstanceIp?? >
    <#list ipsecClients as client>
        <#if client.host.id == currentHost.id >
spdadd ${client.ipAddress.address}/32 ${agentInstanceIp}/32 any -P in priority def+1 none;
spdadd ${client.subnet.networkAddress}/${client.subnet.cidrSize} ${client.ipAddress.address}/32 any -P in ipsec esp/tunnel/0.0.0.0-0.0.0.0/require;
        <#else>
spdadd ${client.subnet.networkAddress}/${client.subnet.cidrSize} ${client.ipAddress.address}/32 any -P out ipsec esp/tunnel/%HOST_IP%-${client.hostIpAddress.address}/require;
# Not really need, here to reduce NOTICE logs saying it's needed
spdadd ${client.ipAddress.address}/32 ${client.subnet.networkAddress}/${client.subnet.cidrSize} any -P in ipsec esp/tunnel/${client.hostIpAddress.address}-%HOST_IP%/require;
        </#if>

    </#list>
</#if>

