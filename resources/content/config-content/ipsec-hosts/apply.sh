#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

IFACE=eth0
IP=$(ifconfig $IFACE | grep 'inet addr:' | awk '{print $2}' | cut -f2 -d:)

add_route_table 200 "iif ${IFACE}"

sed -i 's/%HOST_IP%/'$IP'/g' content-home/etc/cattle/setkey

apply_config ip -batch etc/cattle/ipsec-tunnel-routes
apply_config /usr/sbin/setkey -f etc/cattle/setkey

stage_files
