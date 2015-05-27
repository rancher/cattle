#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

# enable haproxy
sed -i -e 's/ENABLED=0/ENABLED=1/g' /etc/default/haproxy

# apply new config
if haproxy -p /var/run/haproxy.pid -f /etc/healthcheck/healthcheck.cfg -sf $(cat /var/run/haproxy.pid); then
    exit 0
else
    exit 1
fi
