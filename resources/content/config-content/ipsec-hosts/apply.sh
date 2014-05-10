#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

IFACE=eth0
IP=$(ifconfig $IFACE | grep 'inet addr:' | awk '{print $2}' | cut -f2 -d:)

if ! ip rule show | cut -f1 -d: | grep -q '^200$'; then
    ip rule add iif ${IFACE} table 200 pref 200
fi

sed -i 's/%HOST_IP%/'$IP'/g' content-home/etc/cattle/setkey

apply_config ip -batch etc/cattle/ipsec-tunnel-routes
apply_config /usr/sbin/setkey -n -f etc/cattle/setkey

stage_files
