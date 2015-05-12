#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

reload_dns()
{
    if [ ! -e /etc/init.d/rancher-dns ]; then
        # rancher-dns is not yet installed
        return
    fi

    PID=$(pidof rancher-dns || true)

    if [ -z "$PID" ]; then
        /etc/init.d/rancher-dns start
    fi

    killall -HUP rancher-dns
}

DNS=

for address in $(cat /etc/resolv.conf  | sed 's/^# nameserver/nameserver/' | grep ^nameserver | awk '{print $2}'); do
    if [ "$address" = "169.254.169.250" ]; then
        continue
    fi

    if [ -z "$DNS" ]; then
        DNS="\"${address}\""
    else
        DNS="${DNS}, \"${address}\""
    fi
done

sed -i "s/PARENT_DNS/$DNS/" content-home/etc/cattle/dns/answers.json

stage_files

ip addr add 169.254.169.250/32 dev eth0 2>/dev/null || true
if ! ip route show | grep -q 169.254.169.254; then
    ip route add 169.254.169.254/32 dev eth0 via $(ip route get 8.8.8.8 | grep via | awk '{print $3}')
fi

reload_dns
