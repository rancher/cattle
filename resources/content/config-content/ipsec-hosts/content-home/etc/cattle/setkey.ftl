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
            <#if client.instance.id == client.agentInstance.id >
# Local agent instance
spdadd ${client.ipAddress.address}/32 ${agentInstanceIp}/32 any -P in priority def+1 none;
            <#else>
# Local client
spdadd ${client.ipAddress.address}/32 ${agentInstanceIp}/32 any -P in priority def+1 none;
spdadd ${client.subnet.networkAddress}/${client.subnet.cidrSize} ${client.ipAddress.address}/32 any -P in ipsec esp/tunnel/0.0.0.0-0.0.0.0/require;
            </#if>
        <#else>
# Remote client
spdadd ${client.subnet.networkAddress}/${client.subnet.cidrSize} ${client.ipAddress.address}/32 any -P out ipsec esp/tunnel/%HOST_IP%-${client.hostIpAddress.address}/require;
# Not really need, here to reduce NOTICE logs saying it's needed
spdadd ${client.ipAddress.address}/32 ${client.subnet.networkAddress}/${client.subnet.cidrSize} any -P in ipsec esp/tunnel/${client.hostIpAddress.address}-%HOST_IP%/require;
# To local agent instance
spdadd ${client.ipAddress.address}/32 ${agentInstanceClient.ipAddress.address}/32 any -P in ipsec esp/tunnel/0.0.0.0-0.0.0.0/require;
        </#if>

    </#list>
</#if>

