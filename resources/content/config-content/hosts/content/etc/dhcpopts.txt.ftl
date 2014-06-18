<#if ! serviceSet?seq_contains("gatewayService") >
	<#list networkClients as client>
		<#if client.macAddress?? && client.ipAddress?? && client.gateway?? && client.instanceId?? >
tag:host${client.instanceId},option:router,${client.gateway}
		</#if>
	</#list>
</#if>
