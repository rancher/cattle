#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

rm -rf /etc/haproxy/certs/*
stage_files

awk '/BEGIN.*PRIVATE KEY/{i++}{print > "/etc/haproxy/certs/server"i".pem"}' /etc/haproxy/certs/certs.pem
rm /etc/haproxy/certs/certs.pem
if [ ! -s /etc/haproxy/certs/default.pem ]; then rm /etc/haproxy/certs/default.pem;fi

if ! haproxy -v | grep -q 'version 1.6'; then
    apt-get update -y && apt-get install -y software-properties-common \
    && add-apt-repository -y ppa:vbernat/haproxy-1.6 \
    && apt-get update && apt-get -y purge haproxy && apt-get -y install haproxy
fi

reload_haproxy /etc/haproxy/haproxy.cfg
