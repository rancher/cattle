#!/bin/bash

LEASES=/var/lib/misc/dnsmasq.leases

if [[ "$1" == init && -e $LEASES ]]; then
    cat $LEASES
else
    echo "$@"
fi
