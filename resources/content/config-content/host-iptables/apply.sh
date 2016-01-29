#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

BRIDGE=docker0
if docker-1.9 version >/dev/null 2>&1; then
    BRIDGE=$(docker-1.9 network inspect bridge | jq -r '.[0].Options["com.docker.network.bridge.name"]')
    if ! ip link show dev $BRIDGE >/dev/null ; then
        BRIDGE=docker0
    fi
fi

MASQ="$(iptables-save | grep -E -- '-A POSTROUTING.* -o '$BRIDGE' -j MASQUERADE' | sed 's/-A POSTROUTING //')"

if [ -n "$MASQ" ]; then
    cat > masq-rules << EOF
-A CATTLE_POSTROUTING -p tcp ${MASQ} --to-ports 1024-65535
-A CATTLE_POSTROUTING -p udp ${MASQ} --to-ports 1024-65535
EOF
    sed -i '/#POSTRULES/r masq-rules' content-home/etc/cattle/host-iptables
fi

sed -i "s/%BRIDGE%/! -i $BRIDGE/g" content-home/etc/cattle/host-iptables

apply_config iptables-restore -n etc/cattle/host-iptables

for i in PRE POST; do
    if ! iptables -t nat -n -L ${i}ROUTING | grep -q CATTLE_${i}ROUTING; then
        iptables -t nat -I ${i}ROUTING -j CATTLE_${i}ROUTING
    fi
done

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
