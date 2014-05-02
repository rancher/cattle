#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

echo 1 > /proc/sys/net/ipv4/ip_forward
iptables-restore -c < content-home/etc/cattle/iptables-save

stage_files
