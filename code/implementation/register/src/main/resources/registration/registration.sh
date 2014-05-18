#!/bin/sh

ID=$(docker run \
    -e CATTLE_ACCESS_KEY="${CATTLE_ACCESS_KEY}" \
    -e CATTLE_SECRET_KEY="${CATTLE_SECRET_KEY}" \
    -e CATTLE_AGENT_IP="${CATTLE_AGENT_IP}" \
    -e CATTLE_URL="${CATTLE_URL}" \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /var:/host/var \
    -d \
    "${CATTLE_AGENT_REGISTER_IMAGE}")

docker logs -f $ID
docker rm -f $ID >/dev/null 2>&1
