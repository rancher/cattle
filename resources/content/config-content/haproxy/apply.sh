#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

reload_haproxy /etc/haproxy/haproxy.cfg
