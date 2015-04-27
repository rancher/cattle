#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

PID=$(pidof rancher-dns || true)

if [ -z "$PID" ]; then
    /etc/init.d/rancher-dns start
fi

killall -HUP rancher-dns
