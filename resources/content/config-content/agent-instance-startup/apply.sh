#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

while read DEV MAC IP; do
    if [ "$(ip link show dev eth0 | grep 'link/ether' | awk '{print $2}')" != "$MAC" ]; then
        info Setting $DEV mac to $MAC
        ip link set dev $DEV address $MAC
    fi

    if ! ip addr show dev $DEV | grep -iq "$IP"; then
        info Assigning $IP to $DEV
        ip addr add dev $DEV $IP
    fi
done < interfaces

get_config --force services

for i in $(cat ${CATTLE_HOME}/services | grep -vE '^(services|agent-instance-startup|configscripts)$'); do
    info Getting $i
    get_config --force $i
done

if [ -e /dev/shm ]; then
    touch /dev/shm/agent-instance-started
fi
