#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

BRIDGE=docker0
if docker-1.9 version >/dev/null 2>&1; then
    BRIDGE=$(docker-1.9 network inspect bridge | jq -r '.[0].Options["com.docker.network.bridge.name"]')
    if ! ip link show dev $BRIDGE >/dev/null ; then
        BRIDGE=docker0
    fi
    BRIDGE_SUBNET=$(docker-1.9 network inspect bridge | jq -r '.[0].IPAM.Config[0].Subnet')
fi

if [ -n "$BRIDGE_SUBNET" ] && [ -n "$BRIDGE" ]; then
    MASQ="-s $BRIDGE_SUBNET ! -o $BRIDGE -j MASQUERADE"
else
    MASQ="$(iptables-save | grep -E -- '-A POSTROUTING.* -o '$BRIDGE' -j MASQUERADE' | sed 's/-A POSTROUTING //')"
fi

if [ -n "$MASQ" ]; then
    cat > masq-rules << EOF
-A CATTLE_POSTROUTING -p tcp ${MASQ} --to-ports 1024-65535
-A CATTLE_POSTROUTING -p udp ${MASQ} --to-ports 1024-65535
EOF
    sed -i '/#POSTRULES/r masq-rules' content-home/etc/cattle/host-iptables
fi

sed -i "s/%BRIDGE%/! -i $BRIDGE/g" content-home/etc/cattle/host-iptables

apply_config iptables-restore -n etc/cattle/host-iptables

if ! iptables -t nat -L | grep Chain | grep -q CATTLE_HOOK_PREROUTING
    iptables -t nat -N CATTLE_HOOK_PREROUTING
fi

if ! iptables -t nat -n -L CATTLE_HOOK_PREROUTING | grep -q CATTLE_PREROUTING; then
    iptables -t nat -A CATTLE_HOOK_PREROUTING -j CATTLE_PREROUTING
fi

if ! iptables -t nat -n -L PREROUTING | grep -q CATTLE_HOOK_PREROUTING; then
    iptables -t nat -I PREROUTING -j CATTLE_HOOK_PREROUTING
fi

if ! iptables -t nat -n -L POSTROUTING | grep -q CATTLE_POSTROUTING; then
    iptables -t nat -I POSTROUTING -j CATTLE_POSTROUTING
fi

if ! iptables -n -L FORWARD | grep -q CATTLE_FORWARD; then
    iptables -I FORWARD -j CATTLE_FORWARD
fi

add_route_table 300

stage_files

# LEGACY: remove.  This was need just for the racoon to charon upgrade.  Also remove iptables
MIGRATE=${CATTLE_HOME}/etc/cattle/host-iptables.migrate
if grep -q 'migrate ipsec' ${CATTLE_HOME}/etc/cattle/host-iptables; then
    if [ ! -e ${MIGRATE} ]; then
        conntrack -D -p udp || true

        touch ${MIGRATE}
    fi
elif [ -e ${MIGRATE} ]; then
    rm $MIGRATE
fi
