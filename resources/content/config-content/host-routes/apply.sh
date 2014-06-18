#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

OUTPUT=content-home/etc/cattle/subnet-bridge-gateway
ARPTABLES=content-home/etc/cattle/host-nat-gateway-arptables

subnet_bridge_gateway()
{
    if [ ! -e $OUTPUT ]; then
        while read UUID SUBNET GATEWAY URI; do
            BRIDGE=$(docker inspect --format '{{.NetworkSettings.Bridge}}' $UUID || true)

            if [[ -z "$BRIDGE" && -n "$URI" && $URI =~ bridge://.* ]]; then
                TEST_BRIDGE=${URI##bridge://}
                if brctl show $TEST_BRIDGE >/dev/null 2>&1; then
                    BRIDGE=$TEST_BRIDGE
                fi
            fi

            if [ -n "$BRIDGE" ]; then
                echo $SUBNET $BRIDGE $GATEWAY
            fi
        done < content-home/etc/cattle/host-routes.in | sort -u > $OUTPUT
    fi

    cat $OUTPUT
}

routes()
{
    subnet_bridge_gateway | while read SUBNET BRIDGE GATEWAY; do
        echo route add $SUBNET dev $BRIDGE table 300
    done
}

gateway()
{
    subnet_bridge_gateway | while read SUBNET BRIDGE GATEWAY; do
        if [ -z "$GATEWAY" ]; then
            continue;
        fi

        RUN_ARPTABLES=true

        if ! ip addr show dev $BRIDGE | grep -q $GATEWAY; then
            ip addr add ${GATEWAY}/${SUBNET##*/} dev $BRIDGE
        fi
    done
}

setup_arptables()
{
    arptables-save | grep -v CATTLE > $ARPTABLES
    cat << EOF >> $ARPTABLES

*filter
:CATTLE_INPUT -

-A INPUT -j CATTLE_INPUT
EOF

    subnet_bridge_gateway | while read SUBNET BRIDGE GATEWAY; do
        if [ -z "$GATEWAY" ]; then
            continue
        fi

        MAC=$(ip link show dev docker0 | grep 'link/ether' | awk '{print $2}')
        if [ -z "$MAC" ]; then
            continue
        fi

        cat << EOF >> $ARPTABLES
-A CATTLE_INPUT --opcode Reply -s ${GATEWAY}/32 --source-mac ! $MAC -j DROP
EOF
    done

    apply_config arptables-restore ${ARPTABLES##content-home/}
}

setup_routes()
{
    cat > content-home/etc/cattle/host-routes << EOF
route flush table 300

$(routes)

route flush cache
EOF

    apply_config ip -batch etc/cattle/host-routes
}


setup_routes
gateway
setup_arptables

stage_files
