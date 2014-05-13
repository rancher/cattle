#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh


find_routes()
{
    while read UUID SUBNET; do
        BRIDGE=$(docker inspect --format '{{.NetworkSettings.Bridge}}' $UUID || true)

        if [ -n "$BRIDGE" ]; then
            echo route add $SUBNET dev $BRIDGE table 300
        fi
    done < content-home/etc/cattle/host-routes.in
}

routes()
{
    find_routes | sort -u
}

cat > content-home/etc/cattle/host-routes << EOF
route flush table 300

$(routes)

route flush cache
EOF

apply_config ip -batch etc/cattle/host-routes

stage_files
