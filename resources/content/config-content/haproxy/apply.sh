#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

if ! haproxy -v | grep -q 'version 1.6'; then
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 1C61B9CD
    echo 'deb http://ppa.launchpad.net/vbernat/haproxy-1.6/ubuntu trusty main' > /etc/apt/sources.list.d/vbernat-haproxy-1_6-trusty.list
    apt-get update -y
    apt-get upgrade -y haproxy
fi

rm -rf /etc/haproxy/certs/*
stage_files

awk '/BEGIN.*PRIVATE KEY/{i++}{print > "/etc/haproxy/certs/server"i".pem"}' /etc/haproxy/certs/certs.pem
rm /etc/haproxy/certs/certs.pem
if [ ! -s /etc/haproxy/certs/default.pem ]; then rm /etc/haproxy/certs/default.pem;fi

reload_haproxy /etc/haproxy/haproxy.cfg
