global
	log 127.0.0.1 local0
    	log 127.0.0.1 local1 notice
        maxconn 4096
        maxpipes 1024
	chroot /var/lib/haproxy
	user haproxy
	group haproxy
	daemon

defaults
	log	global
	mode	tcp
	option	tcplog
        option  dontlognull
        option  redispatch
        option forwardfor
        option httpclose
        retries 3
        contimeout 5000
        clitimeout 50000
        srvtimeout 50000


<#if healthCheckEntries?has_content>
<#assign publicPort = 300>  
<#list healthCheckEntries as healthCheckEntry>
<#assign protocol = "tcp">  
<#if healthCheckEntry.healthCheck.requestLine??>
<#assign protocol = "http">  
</#if>

frontend ${publicPort}_frontend
        bind ${publicIp.address}:${publicPort}
        mode ${protocol}
        default_backend ${publicPort}_backend

backend ${publicPort}_backend
        mode ${protocol}
        balance roundrobin
        <#if healthCheckEntry.healthCheck.responseTimeout??>timeout check ${healthCheckEntry.healthCheck.responseTimeout}</#if>
        <#if healthCheckEntry.healthCheck.requestLine?? && healthCheckEntry.healthCheck.requestLine?has_content>option httpchk ${healthCheckEntry.healthCheck.requestLine}</#if>
        <#list healthCheckEntry.targetIpAddresses as target>
    	server ${target.address} ${target.address}:${healthCheckEntry.healthCheck.port} check port ${healthCheckEntry.healthCheck.port}<#if healthCheckEntry.healthCheck.interval??> inter ${healthCheckEntry.healthCheck.interval}</#if><#if healthCheckEntry.healthCheck.healthyThreshold??> rise ${healthCheckEntry.healthCheck.healthyThreshold}</#if><#if healthCheckEntry.healthCheck.unhealthyThreshold??> fall ${healthCheckEntry.healthCheck.unhealthyThreshold}</#if>
		</#list>
<#assign publicPort = publicPort + 1>
</#list>
<#else>
listen web 0.0.0.0:9
</#if>
