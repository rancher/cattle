#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

if [ ! -w /etc/hosts ]; then
    mv content/etc/hosts{,.dnsmasq}
fi

stage_files

DNSMASQ_PID=$(pidof dnsmasq)

if [ -n "$DNSMASQ_PID" ]; then
    info Reloading dnsmasq
    kill -HUP $DNSMASQ_PID
fi
