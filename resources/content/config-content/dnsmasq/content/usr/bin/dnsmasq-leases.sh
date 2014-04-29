#!/bin/bash

if [[ "$1" == init && -e /var/lib/dhcp/dhclient.leases ]]; then
    cat /var/lib/dhcp/dhclient.leases
else
    echo "$@"
fi
