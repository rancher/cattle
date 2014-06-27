#!/bin/bash
set -e

./build-image.sh

if [ "$#" -gt 0 ]; then
    URL=$1
    shift 1
fi

docker run \
    --privileged \
    -v /lib/modules:/lib/modules \
    -v /var:/host/var \
    -v /run:/host/run \
    -v /proc:/host/proc \
    -e CATTLE_AGENT_IP="$(ip route get 8.8.8.8 | grep via | awk '{print $7}')" \
    -e CATTLE_SCRIPT_DEBUG=true \
    -e CATTLE_REGISTRATION_URL=$URL \
    -e CATTLE_EXEC_AGENT=true \
    cattle/agent:dev "$@"
