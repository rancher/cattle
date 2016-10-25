global
	log 127.0.0.1 local0
    	log 127.0.0.1 local1 notice
        maxconn 4096
        maxpipes 1024
<#if sslProto?? && sslProto == true>
	tune.ssl.default-dh-param 8192
	ssl-default-bind-ciphers ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!aECDH:!EDH-DSS-DES-CBC3-SHA:!EDH-RSA-DES-CBC3-SHA:!KRB5-DES-CBC3-SHA
	ssl-default-server-ciphers ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!aECDH:!EDH-DSS-DES-CBC3-SHA:!EDH-RSA-DES-CBC3-SHA:!KRB5-DES-CBC3-SHA
	ssl-default-bind-options no-sslv3 no-tls-tickets
	ssl-default-server-options no-sslv3 no-tls-tickets
</#if>
	chroot /var/lib/haproxy
	user haproxy
	group haproxy
	daemon
	<#if haproxyConfig?? && haproxyConfig.global??>
	${haproxyConfig.global}
	</#if>

defaults
	log	global
	mode	tcp
	option	tcplog
        option  dontlognull
        option  redispatch
        option 	http-server-close
        option 	forwardfor
        retries 3
        timeout connect 5000
        timeout client 50000
        timeout server 50000
	errorfile 400 /etc/haproxy/errors/400.http
	errorfile 403 /etc/haproxy/errors/403.http
	errorfile 408 /etc/haproxy/errors/408.http
	errorfile 500 /etc/haproxy/errors/500.http
	errorfile 502 /etc/haproxy/errors/502.http
	errorfile 503 /etc/haproxy/errors/503.http
	errorfile 504 /etc/haproxy/errors/504.http
	<#if haproxyConfig?? && haproxyConfig.defaults??>
	${haproxyConfig.defaults}
	</#if>

<#if listeners?has_content && backends?has_content>
<#list listeners as listener >
<#if listener.privatePort??><#assign sourcePort = listener.privatePort><#else><#assign sourcePort = listener.sourcePort></#if>
<#assign protocol=listener.sourceProtocol>
<#if listener.sourceProtocol == "https"><#assign protocol="http"></#if>
<#if listener.sourceProtocol == "ssl"><#assign protocol="tcp"></#if>
frontend ${listener.uuid}_frontend
        bind *:${sourcePort} <#if listener.proxyPort == true>accept-proxy </#if><#if (listener.sourceProtocol == "https" || listener.sourceProtocol == "ssl") && certs?has_content> ssl crt /etc/haproxy/certs/<#if !defaultCert??> strict-sni</#if></#if>
        mode ${protocol}

        <#list backends[listener.uuid] as backend >
        <#if (listener.sourceProtocol == "http" || listener.sourceProtocol == "https") && (backend.portSpec.domain != "default" || backend.portSpec.path != "default")>
        <#if backend.portSpec.domain != "default">
        <#assign length=backend.portSpec.domain?length>
            <#if (length > 2) && backend.portSpec.domain[0..1] == "*.">
                acl ${backend.uuid}_host hdr_end(host) -i ${backend.portSpec.domain[1..]}
                acl ${backend.uuid}_host hdr_end(host) -i ${backend.portSpec.domain[1..]}:${sourcePort}
            <#elseif (length > 2) && backend.portSpec.domain[length-2..length-1] == ".*">
                acl ${backend.uuid}_host hdr_beg(host) -i ${backend.portSpec.domain[0..length-2]}
                acl ${backend.uuid}_host hdr_beg(host) -i ${backend.portSpec.domain[0..length-2]}:${sourcePort}
            <#else>
                acl ${backend.uuid}_host hdr(host) -i ${backend.portSpec.domain}
                acl ${backend.uuid}_host hdr(host) -i ${backend.portSpec.domain}:${sourcePort}
            </#if>
    	</#if>
    	<#if backend.portSpec.path != "default">
        	acl ${backend.uuid}_path path_beg -i ${backend.portSpec.path}
    	</#if>
    	use_backend ${listener.uuid}_${backend.uuid}_backend if <#if backend.portSpec.domain != "default">${backend.uuid}_host</#if> <#if backend.portSpec.path != "default">${backend.uuid}_path</#if>
        <#elseif backend.portSpec.domain == "default" && backend.portSpec.path == "default">
    	default_backend ${listener.uuid}_${backend.uuid}_backend
        </#if>
        </#list>

<#list backends[listener.uuid]  as backend >
backend ${listener.uuid}_${backend.uuid}_backend
        mode ${protocol}
        <#if backend.healthCheck??>
        <#if backend.healthCheck.responseTimeout??>timeout check ${backend.healthCheck.responseTimeout}</#if>
        <#if backend.healthCheck.requestLine?? && backend.healthCheck.requestLine?has_content>option httpchk ${backend.healthCheck.requestLine}</#if>
        </#if>
        <#if protocol="http">
        <#if lbPolicy??>
        cookie <#if lbPolicy.cookie??>${lbPolicy.cookie}<#else>lbCookie_${listener.uuid}</#if><#if lbPolicy.mode??> ${lbPolicy.mode}<#else> insert</#if><#if lbPolicy.indirect> indirect</#if><#if lbPolicy.nocache> nocache</#if><#if lbPolicy.postonly> postonly</#if><#if lbPolicy.domain?? && lbPolicy.domain?has_content> domain ${lbPolicy.domain}</#if>
        </#if>
        </#if>
        <#list backend.targets as target >
        server ${target.name} ${target.ipAddress}:${target.portSpec.port}<#if target.healthCheck??> check<#if target.healthCheck.port??> port ${target.healthCheck.port}</#if><#if target.healthCheck.interval??> inter ${target.healthCheck.interval}</#if><#if target.healthCheck.healthyThreshold??> rise ${target.healthCheck.healthyThreshold}</#if><#if target.healthCheck.unhealthyThreshold??> fall ${target.healthCheck.unhealthyThreshold}</#if></#if><#if protocol="http" && lbPolicy??> cookie ${target.cookie}</#if>
        </#list>
         <#if listener.sourceProtocol == "https">
        http-request add-header X-Forwarded-Proto https if { ssl_fc }
        </#if>
        http-request add-header X-Forwarded-Port %[dst_port]
        
</#list>
</#list>
</#if>

listen default
<#if lbHealthCheck??><#assign defaultPort = lbHealthCheck.port><#else><#assign defaultPort = 42></#if>
	bind *:${defaultPort}

