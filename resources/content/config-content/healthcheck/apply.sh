#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

chmod +x content/etc/init.d/haproxy-monitor

stage_files

reload_haproxy /etc/healthcheck/healthcheck.cfg
