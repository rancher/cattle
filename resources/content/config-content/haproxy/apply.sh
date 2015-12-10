#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh
. merge-configs.sh

rm -rf /etc/haproxy/certs/*
stage_files

awk '/BEGIN.*PRIVATE KEY/{i++}{print > "/etc/haproxy/certs/server"i".pem"}' /etc/haproxy/certs/certs.pem
rm /etc/haproxy/certs/certs.pem
if [ ! -s /etc/haproxy/certs/default.pem ]; then rm /etc/haproxy/certs/default.pem;fi

if ! haproxy -v | grep -q 'version 1.5'; then
    echo deb http://archive.ubuntu.com/ubuntu trusty-backports main universe > /etc/apt/sources.list.d/backports.list
    apt-get update && apt-get install -y haproxy -t trusty-backports
fi

merge_configs /etc/haproxy/haproxy-default.cfg /etc/haproxy/haproxy-custom.cfg /etc/haproxy/haproxy.cfg

reload_haproxy /etc/haproxy/haproxy.cfg
