global
    log 127.0.0.1 local0
    log 127.0.0.1 local1 notice
    maxconn 4096
    maxpipes 1024
    chroot /var/lib/haproxy
    user haproxy
    group haproxy
    daemon
    stats socket /var/run/haproxy.sock mode 600 level admin
    stats timeout 2m

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


<#list healthCheckEntries as healthCheckEntry>
<#assign protocol = "tcp">  
<#if healthCheckEntry.healthCheck.requestLine??>
<#assign protocol = "http">  
</#if>

backend ${healthCheckEntry.healthCheckUuid}_backend
        mode ${protocol}
        balance roundrobin
        <#if healthCheckEntry.healthCheck.responseTimeout??>timeout check ${healthCheckEntry.healthCheck.responseTimeout}</#if>
        <#if healthCheckEntry.healthCheck.requestLine?? && healthCheckEntry.healthCheck.requestLine?has_content>option httpchk ${healthCheckEntry.healthCheck.requestLine}</#if>
        server cattle-${healthCheckEntry.healthCheckUuid} ${healthCheckEntry.targetIpAddress.address}:${healthCheckEntry.healthCheck.port} check port ${healthCheckEntry.healthCheck.port}<#if healthCheckEntry.healthCheck.interval??> inter ${healthCheckEntry.healthCheck.interval}</#if><#if healthCheckEntry.healthCheck.healthyThreshold??> rise ${healthCheckEntry.healthCheck.healthyThreshold}</#if><#if healthCheckEntry.healthCheck.unhealthyThreshold??> fall ${healthCheckEntry.healthCheck.unhealthyThreshold}</#if>
</#list>

# Need to listen on something since we have no front ends
listen web 127.0.0.1:42
