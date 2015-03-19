{
    "default": {
        "recurse": ["8.8.8.8"],
        "rancher.com.": ["1.2.3.4"]
    },
<#if dnsEntries?? && dnsEntries?has_content>
    <#list dnsEntries as dnsEntry>
        <#if dnsEntry.resolve?has_content>
    "${dnsEntry.sourceIpAddress.address}": {
                <#list dnsEntry.resolve?keys as dnsName>
                  <#if dnsEntry.resolve[dnsName]?? && dnsEntry.resolve[dnsName]?has_content>
        "${dnsName}.": [<#list dnsEntry.resolve[dnsName] as address>"${address.address}"<#if address_has_next>, </#if></#list>]<#if dnsName_has_next>,</#if>
                    </#if>
                </#list>
    }<#if dnsEntry_has_next>, </#if>
        </#if>
</#list>
</#if>
}
