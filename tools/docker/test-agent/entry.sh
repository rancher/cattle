#!/bin/bash
set -e

if [ -n "$SSH_PORT" ]; then
    iptables -t nat -I PREROUTING -i eth0 -p tcp --dport 22 -j DNAT --to ${SSH_PORT_22_TCP_ADDR}:${SSH_PORT_22_TCP_PORT}
    iptables -t nat -I POSTROUTING -o eth0 -p tcp --dport ${SSH_PORT_22_TCP_PORT} -d ${SSH_PORT_22_TCP_ADDR} -j MASQUERADE
fi

exec "$@"
