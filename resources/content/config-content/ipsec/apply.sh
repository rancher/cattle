#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

ARPDEV=arpproxy
NETDEV=eth0
TABLE=200

delete_dummy()
{
    ip link set dev $ARPDEV down || true
    ip link delete dev $ARPDEV || true
}

disable_proxyarp()
{
    echo 0 > /proc/sys/net/ipv4/conf/$NETDEV/proxy_arp
}

route_tables()
{
    if ! ip rule list | cut -f1 -d: | grep -q 200; then
        ip rule del iif $NETDEV table $TABLE pref 200 || true
    fi
}

ipsec_settings()
{
    echo 65536 > /proc/sys/net/ipv4/xfrm4_gc_thresh || true
}

delete_dummy
disable_proxyarp
route_tables
ipsec_settings

stage_files

rm -f /etc/monit/conf.d/racoon

reload_monit
/etc/init.d/racoon stop || true
