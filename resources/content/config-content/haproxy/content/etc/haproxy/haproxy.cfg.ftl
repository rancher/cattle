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
frontend ${listener.name}_frontend
        bind ${publicIp}:${listener.sourcePort}
        mode ${listener.sourceProtocol}
        default_backend ${listener.name}_backend

backend ${listener.name}_backend
        mode ${listener.targetProtocol}
        balance ${listener.data.fields.algorithm}
        <#list targets as target >
        server ${target.name} ${target.ipAddress}:${listener.targetPort}
        </#list>

</#list>
<#else>
listen web 0.0.0.0:9
</#if>
