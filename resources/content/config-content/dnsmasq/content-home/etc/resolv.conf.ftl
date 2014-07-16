<#list services?values as service >
    <#if service.kind == "dnsService" && (service.service.data.fields.dns)?? >
        <#list service.service.data.fields.dns as dns >
nameserver ${dns}
        </#list>
        <#break>
    </#if>
</#list>
