#!/bin/sh

export CATTLE_REGISTRATION_ACCESS_KEY="${CATTLE_REGISTRATION_ACCESS_KEY}"
export CATTLE_REGISTRATION_SECRET_KEY="${CATTLE_REGISTRATION_SECRET_KEY}"
export CATTLE_URL="${CATTLE_URL}"

if [ "$CATTLE_RUN_REGISTRATION" != "false" ]; then
    ID=$(docker run \
        --name cattle-agent \
        --privileged \
        -e CATTLE_REGISTRATION_ACCESS_KEY="${CATTLE_REGISTRATION_ACCESS_KEY}" \
        -e CATTLE_REGISTRATION_SECRET_KEY="${CATTLE_REGISTRATION_SECRET_KEY}" \
        -e CATTLE_AGENT_IP="${CATTLE_AGENT_IP}" \
        -e CATTLE_URL="${CATTLE_URL}" \
        -v /lib/modules:/host/lib/modules:ro \
        -v /var/lib/docker:/host/var/lib/docker \
        -v /var/lib/cattle:/host/var/lib/cattle \
        -v /opt/bin:/host/opt/bin \
        -v /proc:/host/proc \
        -v /run:/host/run \
        -d \
        "${CATTLE_AGENT_IMAGE}")

    echo "Launched container $ID, run \"docker logs -f $ID\" for progress"
fi
