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
        	<#else>
        "recurse": [
            PARENT_DNS
        ],
        	</#if>
            <#if dnsEntry.resolve?has_content>
        "a": {
        	"rancher-metadata.": {"answer": ["169.254.169.250"]},
		"rancher-metadata.rancher.internal.": {"answer": ["169.254.169.250"]},
                <#list dnsEntry.resolve?keys as dnsName>
                  <#if dnsEntry.resolve[dnsName]?? && dnsEntry.resolve[dnsName]?has_content>
		"${dnsName?lower_case}.": {"answer": [<#list dnsEntry.resolve[dnsName] as address><#if address??>"${address}"<#if address_has_next>, </#if></#if></#list>]},
		"${dnsName?lower_case}.rancher.internal.": {"answer": [<#list dnsEntry.resolve[dnsName] as address><#if address??>"${address}"<#if address_has_next>, </#if></#if></#list>]}<#if dnsName_has_next>,</#if>
                    </#if>
                </#list>
    	}</#if><#if dnsEntry.resolveCname?has_content>,
    	"cname": {
                <#list dnsEntry.resolveCname?keys as dnsName>
                  <#if dnsEntry.resolveCname[dnsName]??>
		"${dnsName?lower_case}.": {"answer": <#if dnsEntry.resolveCname[dnsName]??>"${dnsEntry.resolveCname[dnsName]}."</#if>},
		"${dnsName?lower_case}.rancher.internal.": {"answer": <#if dnsEntry.resolveCname[dnsName]??>"${dnsEntry.resolveCname[dnsName]}."</#if>}<#if dnsName_has_next>,</#if>
                    </#if>
                </#list>
    	}</#if>

    },

        </#if>
    </#list>
    "": {}
</#if>
}
