#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

ARPDEV=arpproxy
NETDEV=eth0
TABLE=200

create_dummy()
{
    if ! ip link show dev $ARPDEV >/dev/null 2>&1; then
        ip link add dev $ARPDEV type dummy
    fi

    ip link set dev $ARPDEV up
}

proxyarp()
{
    for i in $ARPDEV $NETDEV; do
        echo 1 > /proc/sys/net/ipv4/conf/$i/proxy_arp
    done
}

route_tables()
{
    if ! ip rule list | cut -f1 -d: | grep -q 200; then
        ip rule add iif $NETDEV table $TABLE pref 200
    fi
}

ipsec_settings()
{
    echo 32768 > /proc/sys/net/ipv4/xfrm4_gc_thresh || true
}

chmod 600 content/etc/racoon/psk.txt

create_dummy
proxyarp
route_tables
ipsec_settings

stage_files

/etc/init.d/racoon restart
