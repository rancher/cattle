<#assign primaryNic = "eth0">

*filter
:INPUT DROP [0:0]
:FORWARD ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]

# Allow established traffic
-A INPUT -i ${primaryNic} -m state --state RELATED,ESTABLISHED -j ACCEPT

# DHCP/DNS
-A INPUT -i ${primaryNic} -p udp --dport 67 -j ACCEPT
-A INPUT -i ${primaryNic} -p udp --dport 53 -j ACCEPT
-A INPUT -i ${primaryNic} -p tcp --dport 53 -j ACCEPT

# IPsec
-A INPUT -i ${primaryNic} -p udp --dport 500 -j ACCEPT
-A INPUT -i ${primaryNic} -p udp --dport 4500 -j ACCEPT

# ICMP
-A INPUT -i ${primaryNic} -p icmp -j ACCEPT

# loopback
-A INPUT -i lo -j ACCEPT

# Node Services
-A INPUT -p tcp --dport 8080 -j ACCEPT

COMMIT

*nat
:PREROUTING ACCEPT [0:0]
:INPUT ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]
:POSTROUTING ACCEPT [0:0]

<#list links as linkData>
    <#assign link = linkData.link>
    <#if link.linkName?? && (link.data.fields.ports)?? >
# Links for link ${link.uuid}
        <#list link.data.fields.ports as port >
            <#if port.privatePort?? && port.publicPort?? && port.protocol?? && port.ipAddress?? && linkData.target.address?? >
-A PREROUTING -i ${primaryNic} -p ${port.protocol} --dport ${port.publicPort} -d ${port.ipAddress} -j MARK --set-mark 100
-A PREROUTING -i ${primaryNic} -p ${port.protocol} --dport ${port.publicPort} -d ${port.ipAddress} -j DNAT --to ${linkData.target.address}:${port.privatePort}
            </#if>
        </#list>
    </#if>
</#list>

-A POSTROUTING -o ${primaryNic} -m mark --mark 100 -j MASQUERADE

COMMIT

*mangle

# Fix DHCP packets
-A POSTROUTING -p udp --dport bootpc -j CHECKSUM --checksum-fill

COMMIT
