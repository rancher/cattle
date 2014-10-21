#!/bin/bash

trap reset SIGTERM

reset()
{
    kill -9 $PID
    exit 1
}

cat > haproxy.cfg << EOF
global
    maxconn 4096
    chroot /var/lib/haproxy
    user haproxy
    group haproxy
	 
defaults
    mode http
    option dontlognull
    option forwardfor
    option redispatch
    retries 3
    timeout connect 5000
    timeout client 120000
    timeout server 120000
    timeout queue 50000
	
listen web 0.0.0.0:${LISTEN_PORT:-80}
    balance leastconn
EOF

i=1
while true; do
    eval SERVER='${TARGET'${i}'_PORT}'
    if [ "$SERVER" = "" ]; then
        break
    else
        HOST=${SERVER##tcp://}
        HOST=${HOST%%:*}
        PORT=${SERVER##*:}
        curl --connect-timeout 1 http://${HOST}:${PORT} >/dev/null 2>&1 || true
        cat >> haproxy.cfg << EOF
    server server${i} ${HOST}:${PORT} check
EOF
    fi

    i=$((i+1))
done

ulimit -n 8096
while true; do
    haproxy -f haproxy.cfg &
    PID=$!
    wait
done
