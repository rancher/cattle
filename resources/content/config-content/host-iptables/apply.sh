#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

MASQ="$(iptables-save | grep -- '-A POSTROUTING.* -o docker0 -j MASQUERADE' | sed 's/-A POSTROUTING //')"

if [ -n "$MASQ" ]; then
   cat > masq-rules << EOF
-A CATTLE_POSTROUTING -p tcp ${MASQ} --to-ports 1024-65535
-A CATTLE_POSTROUTING -p udp ${MASQ} --to-ports 1024-65535
EOF
   sed -i '/#POSTRULES/r masq-rules' content-home/etc/cattle/host-iptables
fi

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

add_route_table 300

stage_files
