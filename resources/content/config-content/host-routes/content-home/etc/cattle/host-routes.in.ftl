<#list routes as route >
${route.instance.uuid} ${route.subnet.networkAddress}/${route.subnet.cidrSize}
</#list>
