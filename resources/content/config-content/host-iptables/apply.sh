#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

if ! iptables -t nat -n -L CATTLE_PREROUTING 2>/dev/null; then
    iptables -t nat -N CATTLE_PREROUTING
fi

apply_config iptables-restore -n etc/cattle/host-iptables

if ! iptables -t nat -L PREROUTING | grep -q CATTLE_PREROUTING; then
    iptables -t nat -I PREROUTING -j CATTLE_PREROUTING
fi

add_route_table 300

stage_files
