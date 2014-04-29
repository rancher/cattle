#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

service dnsmasq restart

for i in {1..100}; do
    if [ "$(pidof dnsmasq)" = "" ]; then
        sleep .1
    else
        break
    fi
done

if [ "$(pidof dnsmasq)" = "" ]; then
    error "Failed to start dnsmasq"
    exit 1
fi
