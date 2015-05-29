{
    "default": {
        "recurse": [
            PARENT_DNS
        ]
    },
<#if dnsEntries?? >
    <#list dnsEntries as dnsEntry>
        <#if dnsEntry.resolve?has_content && dnsEntry.sourceIpAddress.address??>
    "${dnsEntry.sourceIpAddress.address}": {
            <#if (dnsEntry.instance.data.fields.dns)?? && dnsEntry.instance.data.fields.dns?has_content >
        "recurse": [
                <#list dnsEntry.instance.data.fields.dns as recurse >
                    <#if recurse_has_next >
            "${recurse}",
                    <#else>
            "${recurse}"
                    </#if>
                </#list>
        ],
            </#if>
                <#list dnsEntry.resolve?keys as dnsName>
                  <#if dnsEntry.resolve[dnsName]?? && dnsEntry.resolve[dnsName]?has_content>
        "${dnsName?lower_case}.": [<#list dnsEntry.resolve[dnsName] as address><#if address??>"${address}"<#if address_has_next>, </#if></#if></#list>],
        "${dnsName?lower_case}.rancher.internal.": [<#list dnsEntry.resolve[dnsName] as address><#if address??>"${address}"<#if address_has_next>, </#if></#if></#list>]<#if dnsName_has_next>,</#if>
                    </#if>
                </#list>
    },
        </#if>
    </#list>
    "": {}
</#if>
}
