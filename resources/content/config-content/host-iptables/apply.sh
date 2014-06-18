#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

apply_config iptables-restore -n etc/cattle/host-iptables

for i in PRE POST; do
    if ! iptables -t nat -n -L ${i}ROUTING | grep -q CATTLE_${i}ROUTING; then
        iptables -t nat -I ${i}ROUTING -j CATTLE_${i}ROUTING
    fi
done

add_route_table 300

stage_files
