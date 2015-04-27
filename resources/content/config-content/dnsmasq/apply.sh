#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

if [ -e /etc/init.d/dnsmasq ]; then
    /etc/init.d/dnsmasq stop || true
    killall -9 dnsmasq || true
fi

if [ -e /etc/monit/conf.d/dnsmasq ]; then
    monit unmonitor dnsmasq || true
    rm /etc/monit/conf.d/dnsmasq
    monit reload
fi
