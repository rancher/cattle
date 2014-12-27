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

for i in PRE POST; do
    if ! iptables -t nat -n -L ${i}ROUTING | grep -q CATTLE_${i}ROUTING; then
        iptables -t nat -I ${i}ROUTING -j CATTLE_${i}ROUTING
    fi
done

add_route_table 300

stage_files
