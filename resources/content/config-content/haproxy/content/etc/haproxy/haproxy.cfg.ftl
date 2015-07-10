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

<#if listeners?has_content && backends?has_content>
<#list listeners as listener >
<#if listener.privatePort??><#assign sourcePort = listener.privatePort><#else><#assign sourcePort = listener.sourcePort></#if>
frontend ${listener.uuid}_frontend
        bind ${publicIp}:${sourcePort}
        mode ${listener.sourceProtocol}
        <#list backends as backend >
        <#if backend.portSpec.sourcePort == sourcePort>
        <#if (listener.sourceProtocol == "http" || listener.sourceProtocol == "https") && (backend.portSpec.domain != "default" || backend.portSpec.path != "default")>
        <#if backend.portSpec.domain != "default">
        acl ${backend.uuid}_host hdr(host) -i ${backend.portSpec.domain}
    	</#if>
    	<#if backend.portSpec.path != "default">
        acl ${backend.uuid}_path path_beg -i ${backend.portSpec.path}
    	</#if>
    	use_backend ${listener.uuid}_${backend.uuid}_backend if <#if backend.portSpec.domain != "default">${backend.uuid}_host</#if> <#if backend.portSpec.path != "default">${backend.uuid}_path</#if>
    	<#else>
    	default_backend ${listener.uuid}_${backend.uuid}_backend
        </#if>
        </#if>
        </#list>

<#list backends as backend >
<#if backend.portSpec.sourcePort == sourcePort>
backend ${listener.uuid}_${backend.uuid}_backend
        mode ${listener.targetProtocol}
        balance ${listener.data.fields.algorithm}
        <#if backend.healthCheck??>
        <#if backend.healthCheck.responseTimeout??>timeout check ${backend.healthCheck.responseTimeout}</#if>
        <#if backend.healthCheck.requestLine?? && backend.healthCheck.requestLine?has_content>option httpchk ${backend.healthCheck.requestLine}</#if>
        </#if>
        <#if listener.targetProtocol="http">
        <#if appPolicy??>
        appsession ${appPolicy.cookie} len ${appPolicy.maxLength} timeout ${appPolicy.timeout}<#if appPolicy.requestLearn> request-learn</#if><#if appPolicy.prefix> prefix</#if><#if appPolicy.mode??> mode <#if appPolicy.mode = "path_parameters">path-parameters<#else>query-string</#if></#if>
        </#if>
        <#if lbPolicy??>
        cookie <#if lbPolicy.cookie??>${lbPolicy.cookie}<#else>lbCookie_${listener.uuid}</#if><#if lbPolicy.mode??> ${lbPolicy.mode}<#else> insert</#if><#if lbPolicy.indirect> indirect</#if><#if lbPolicy.nocache> nocache</#if><#if lbPolicy.postonly> postonly</#if><#if lbPolicy.domain?? && lbPolicy.domain?has_content> domain ${lbPolicy.domain}</#if>
        </#if>
        </#if>
        <#list backend.targets as target >
        server ${target.name} ${target.ipAddress}:${target.portSpec.port}<#if target.healthCheck??> check<#if target.healthCheck.port??> port ${target.healthCheck.port}</#if><#if target.healthCheck.interval??> inter ${target.healthCheck.interval}</#if><#if target.healthCheck.healthyThreshold??> rise ${target.healthCheck.healthyThreshold}</#if><#if target.healthCheck.unhealthyThreshold??> fall ${target.healthCheck.unhealthyThreshold}</#if></#if><#if listener.targetProtocol="http" && lbPolicy??> cookie ${target.cookie}</#if>
        </#list>
</#if>
</#list>
</#list>
<#else>
listen web 0.0.0.0:9
</#if>
