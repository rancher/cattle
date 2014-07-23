<#list routes as route >
    <#if route.instance.kind == "container" >
${route.instance.uuid} ${route.subnet.networkAddress}/${route.subnet.cidrSize}<#if (route.hostNatGatewayService)?? > ${route.subnet.gateway} ${(route.vnet.uri)!""}</#if>
    </#if>
</#list>
