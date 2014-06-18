<#list routes as route >
${route.instance.uuid} ${route.subnet.networkAddress}/${route.subnet.cidrSize}<#if (route.hostNatGatewayService)?? > ${route.subnet.gateway} ${(route.vnet.uri)!""}</#if>
</#list>
