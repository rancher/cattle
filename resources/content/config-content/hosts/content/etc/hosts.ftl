<#list networkClients as client>
    <#if client.macAddress?? && client.ipAddress?? >
        <#if client.domain?? >
            <#assign domain = client.domain>
        <#elseif client.networkDomain?? >
            <#assign domain = client.networkDomain >
        <#else>
            <#assign domain = client.defaultDomain >
        </#if>
        <#if client.instanceId == instance.id >
127.0.0.1 ${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"} ${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"}.${domain}
        <#else>
${client.ipAddress} ${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"} ${hostname!"i-${client.macAddress?replace(\":\",\"-\")}"}.${domain} localhost localhost.localdomain
        </#if>
    </#if>
</#list>
::1 localhost ip6-localhost ip6-loopback
fe00::0 ip6-localnet
ff00::0 ip6-mcastprefix
ff02::1 ip6-allnodes
ff02::2 ip6-allrouters
ff02::3 ip6-allhosts
