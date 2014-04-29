<#list instance.nics as nic >
    <#list nic.ipAddresses as ip >
        <#if ip.role?? && ip.role == "primary" && (ip.subnet.cidrSize)?? && nic.macAddress?? >
eth${nic.deviceNumber!0} ${nic.macAddress} ${ip.address}/${ip.subnet.cidrSize}
        </#if>
    </#list>
</#list>
