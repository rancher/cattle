#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

echo 1 > /proc/sys/net/ipv4/ip_forward
echo 0 > /proc/sys/net/ipv4/conf/all/rp_filter
echo 0 > /proc/sys/net/ipv4/conf/eth0/rp_filter

if uname -r | grep -q '^2.6'; then
    # 2.6.32 does not support INPUT on nat chain
    sed -e '/^.nat/,${/INPUT/d}' content-home/etc/cattle/iptables-save | iptables-restore -c
else
    iptables-restore -c < content-home/etc/cattle/iptables-save
fi

stage_files
