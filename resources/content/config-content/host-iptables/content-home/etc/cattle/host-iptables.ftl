*nat

-F CATTLE_PREROUTING
<#if ports?? >
    <#list ports as mapping>
        <#if (mapping.publicIpAddress.address)?? && (mapping.privateIpAddress.address)?? && (mapping.port.publicPort)?? && (mapping.port.privatePort)?? >
-I CATTLE_PREROUTING -p tcp -d ${mapping.publicIpAddress.address} --dport ${mapping.port.publicPort} -j DNAT --to ${mapping.privateIpAddress.address}:${mapping.port.privatePort}
        </#if>
    </#list>
</#if>

COMMIT

