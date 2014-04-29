<#if ! serviceSet?seq_contains("gatewayService") >
	<#list networkClients as client>
		<#if client.macAddress?? && client.ipAddress?? && client.gateway?? && client.instanceId?? >
dhcp-option=tag:host${client.instanceId}, option:router, ${client.gateway}
		</#if>
	</#list>
</#if>
