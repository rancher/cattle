#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

if ! haproxy -v | grep -q 'version 1.6'; then
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 1C61B9CD
    echo 'deb http://ppa.launchpad.net/vbernat/haproxy-1.6/ubuntu trusty main' > /etc/apt/sources.list.d/vbernat-haproxy-1_6-trusty.list
    apt-get update -y
    apt-get upgrade -y haproxy
fi

chmod +x content/etc/init.d/haproxy-monitor

stage_files

reload_haproxy /etc/healthcheck/healthcheck.cfg
