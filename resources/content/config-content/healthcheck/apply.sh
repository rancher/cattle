#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

chmod +x content/etc/init.d/haproxy-monitor

stage_files

if ! haproxy -v | grep -q 'version 1.6'; then
    apt-get update -y && apt-get install -y software-properties-common \
    && add-apt-repository -y ppa:vbernat/haproxy-1.6 \
    && apt-get update && apt-get -y purge haproxy && apt-get -y install haproxy
fi

reload_haproxy /etc/healthcheck/healthcheck.cfg
