#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

OUTPUT=content-home/etc/cattle/subnet-bridge-gateway

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
        echo route add $SUBNET dev $BRIDGE src $GATEWAY table 300
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

setup_routes()
{
    cat > content-home/etc/cattle/host-routes << EOF
route flush table 300

$(routes)

route flush cache
EOF

    apply_config ip -batch etc/cattle/host-routes
}

get_assigned_ips()
{
    ip addr show dev $1 | grep ':cattle' | awk '{print $2}' | cut -f1 -d'/'
}

setup_ips()
{
    declare -A BY_IFACE
    declare -A ASSIGNED

    mapfile HOST_IPS < content-home/etc/cattle/host-ip

    for line in "${HOST_IPS[@]}"; do
        set $line
        IFACE=$1
        IP_ADDRESS=$2

        BY_IFACE[$IFACE]="${BY_IFACE[$IFACE]} $IP_ADDRESS"
    done

    for KEY in "${!BY_IFACE[@]}"; do
        if [ "$KEY" = "default" ]; then
            IFACE=$(ip route get 8.8.8.8 | grep via | awk '{print $5}')
        else
            IFACE=$KEY
        fi

        if [ -z "$IFACE" ]; then
            continue
        fi

        for IP in $(get_assigned_ips $IFACE); do
            ASSIGNED[$IP]="existing"
        done

        for IP in ${BY_IFACE[$KEY]}; do
            if [ "${ASSIGNED[$IP]}" != "existing" ]; then
                info Adding ${IP}/32 to $IFACE
                ip addr add ${IP}/32 dev $IFACE label ${IFACE}:cattle
            else
                info ${IP}/32 on $IFACE already assigned
            fi
            ASSIGNED[$IP]="true"
        done

        for IP in "${!ASSIGNED[@]}"; do
            if [ "${ASSIGNED[$IP]}" = "existing" ]; then
                info Removing $IP from $IFACE
                ip addr del ${IP}/32 dev $IFACE
            fi
        done
    done
}


gateway
setup_routes
setup_ips

stage_files
