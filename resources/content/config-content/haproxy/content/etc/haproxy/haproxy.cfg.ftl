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
	errorfile 400 /etc/haproxy/errors/400.http
	errorfile 403 /etc/haproxy/errors/403.http
	errorfile 408 /etc/haproxy/errors/408.http
	errorfile 500 /etc/haproxy/errors/500.http
	errorfile 502 /etc/haproxy/errors/502.http
	errorfile 503 /etc/haproxy/errors/503.http
	errorfile 504 /etc/haproxy/errors/504.http

<#if listeners?has_content && targets?has_content>
<#list listeners as listener >
frontend ${listener.uuid}_frontend
        bind ${publicIp}:${listener.sourcePort}
        mode ${listener.sourceProtocol}
        default_backend ${listener.uuid}_backend

backend ${listener.uuid}_backend
        mode ${listener.targetProtocol}
        balance ${listener.data.fields.algorithm}
        <#if healthCheck??>
        <#if healthCheck.responseTimeout??>timeout check ${healthCheck.responseTimeout}</#if>
        <#if healthCheck.uri?? && healthCheck.uri?has_content>option httpchk ${healthCheck.uri}</#if>
        </#if>
        <#if listener.targetProtocol="http">
        <#if appPolicy??>
        appsession <#if appPolicy.cookie??>${appPolicy.cookie}<#else>appCookie_listener.uuid</#if><#if appPolicy.length??> len ${appPolicy.length}</#if><#if appPolicy.timeout??> timeout ${appPolicy.timeout}</#if><#if appPolicy.requestLearn> request-learn</#if><#if appPolicy.prefix> prefix</#if><#if appPolicy.mode??> mode <#if appPolicy.mode = "path_parameters">path-parameters<#else>query-string</#if></#if>
        </#if>
        <#if lbPolicy??>
        cookie <#if lbPolicy.cookie??>${lbPolicy.cookie}<#else>lbCookie_listener.uuid</#if><#if lbPolicy.mode??> ${lbPolicy.mode}<#else> insert</#if><#if lbPolicy.indirect> indirect</#if><#if lbPolicy.nocache> nocache</#if><#if lbPolicy.postonly> postonly</#if><#if lbPolicy.domain?? && lbPolicy.domain?has_content> domain ${lbPolicy.domain}</#if>
        </#if>
        </#if>
        <#list targets as target >
        server ${target.name} ${target.ipAddress}:${listener.targetPort}<#if healthCheck??> check<#if healthCheck.interval??> inter ${healthCheck.interval}</#if><#if healthCheck.healthyThreshold??> rise ${healthCheck.healthyThreshold}</#if><#if healthCheck.unhealthyThreshold??> fall ${healthCheck.unhealthyThreshold}</#if></#if><#if listener.targetProtocol="http" && lbPolicy??> cookie ${target.cookie}</#if>
        </#list>

</#list>
<#else>
listen web 0.0.0.0:9
</#if>
