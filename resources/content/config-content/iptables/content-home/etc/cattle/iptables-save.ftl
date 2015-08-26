<#assign primaryNic = "eth0">

*filter
:INPUT DROP [0:0]
:FORWARD ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]

# Allow established traffic
-A INPUT -i ${primaryNic} -m state --state RELATED,ESTABLISHED -j ACCEPT

# DNS
-A INPUT -i ${primaryNic} -p udp --dport 53 -j ACCEPT
-A INPUT -i ${primaryNic} -p tcp --dport 53 -j ACCEPT

# IPsec
-A INPUT -i ${primaryNic} -p udp --dport 500 -j ACCEPT
-A INPUT -i ${primaryNic} -p udp --dport 4500 -j ACCEPT

# ICMP
-A INPUT -i ${primaryNic} -p icmp -j ACCEPT

# Metadata
-A INPUT -i ${primaryNic} -p tcp --dport 80 -j ACCEPT

# loopback
-A INPUT -i lo -j ACCEPT

COMMIT

*nat
:PREROUTING ACCEPT [0:0]
:OUTPUT ACCEPT [0:0]
:POSTROUTING ACCEPT [0:0]

<#list subnets as subnet >
-A POSTROUTING -s ${subnet.gateway}/32 -o ${primaryNic} -j MASQUERADE
</#list>
COMMIT

*mangle

# Fix DHCP packets
-A POSTROUTING -p udp --dport bootpc -j CHECKSUM --checksum-fill

COMMIT
