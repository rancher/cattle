*nat

:CATTLE_PREROUTING -
:CATTLE_POSTROUTING -

-F CATTLE_PREROUTING
-F CATTLE_POSTROUTING

<#if ports?? >
    <#list ports as mapping>
        <#if (mapping.publicIpAddress.address)?? && (mapping.privateIpAddress.address)?? && (mapping.port.publicPort)?? && (mapping.port.privatePort)?? >
# TODO Support multiple host IP's so add -d ${mapping.publicIpAddress.address} but make it work for EC2 also.  EC2 is more difficult because the destination IP is not really known to Cattle
-A CATTLE_PREROUTING -p ${mapping.port.protocol} -m addrtype --dst-type LOCAL --dport ${mapping.port.publicPort} -j DNAT --to ${mapping.privateIpAddress.address}:${mapping.port.privatePort}
        </#if>
    </#list>
</#if>

<#assign previous = "" >
<#list routes as route>
    <#if route.hostNatGatewayService?? && (route.subnet.gateway)?? && previous != route.subnet.networkAddress >
        <#assign previous = route.subnet.networkAddress >
-A CATTLE_POSTROUTING -s ${route.subnet.networkAddress}/${route.subnet.cidrSize} ! -d ${route.subnet.networkAddress}/${route.subnet.cidrSize} -j MASQUERADE
    </#if>
</#list>

<#list metadataRedirects as redirect >
-A CATTLE_PREROUTING -s ${redirect.subnet.networkAddress}/${redirect.subnet.cidrSize} -d 169.254.169.254 -j DNAT --to ${redirect.ipAddress.address}
</#list>

COMMIT

