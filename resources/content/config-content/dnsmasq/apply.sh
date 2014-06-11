#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

mkdir -p /run/resolvconf/interface

stage_files

if /etc/init.d/dnsmasq status; then
    # This weirdness is because dnsmasq restart sleeps for 2 seconds, at which time
    # monit usually restarts it.  So the restart fails because it looks like it never
    # stopped, but it did actually restart
    /etc/init.d/dnsmasq restart || /etc/init.d/dnsmasq status
else
    /etc/init.d/dnsmasq start
fi

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
