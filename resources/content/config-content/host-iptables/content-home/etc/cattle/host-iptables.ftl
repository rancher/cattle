*nat

-F CATTLE_PREROUTING
<#if ports?? >
    <#list ports as mapping>
        <#if (mapping.publicIpAddress.address)?? && (mapping.privateIpAddress.address)?? && (mapping.port.publicPort)?? && (mapping.port.privatePort)?? >
# TODO Support multiple host IP's so add -d ${mapping.publicIpAddress.address} but make it work for EC2 also
-I CATTLE_PREROUTING -p ${mapping.port.protocol} -m addrtype --dst-type LOCAL --dport ${mapping.port.publicPort} -j DNAT --to ${mapping.privateIpAddress.address}:${mapping.port.privatePort}
        </#if>
    </#list>
</#if>

COMMIT

