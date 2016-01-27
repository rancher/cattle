#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

reload_haproxy /etc/haproxy/haproxy.cfg
