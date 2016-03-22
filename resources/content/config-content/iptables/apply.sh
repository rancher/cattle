#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

echo 1 > /proc/sys/net/ipv4/ip_forward
echo 0 > /proc/sys/net/ipv4/conf/all/rp_filter
echo 0 > /proc/sys/net/ipv4/conf/eth0/rp_filter
echo 120 > /proc/sys/net/ipv4/neigh/eth0/delay_first_probe_time

iptables-restore -c < content-home/etc/cattle/iptables-save
arp -f content-home/etc/cattle/ethers

stage_files
