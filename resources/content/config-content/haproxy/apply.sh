#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

awk '/BEGIN RSA PRIVATE KEY/{i++}{print > "/etc/haproxy/certs/server"i".pem"}' /etc/haproxy/certs/certs.pem
rm /etc/haproxy/certs/certs.pem
if [ ! -s /etc/haproxy/certs/default.pem ]; then rm /etc/haproxy/certs/default.pem;fi

reload_haproxy /etc/haproxy/haproxy.cfg
