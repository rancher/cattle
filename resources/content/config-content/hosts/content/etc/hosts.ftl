<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
        <#if client.instanceId == instance.id >
127.0.0.1 localhost localhost.localdomain ${client.hostname} ${client.fqdn}
        <#else>
${client.ipAddress} ${client.hostname} ${client.fqdn}
        </#if>
    </#if>
</#list>
::1 localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
ff02::3 ip6-allhosts
