#!/bin/bash
set -e -x

DEV_HOST=${DEV_HOST:-10.0.2.2:8080}
AGENT_IMAGE=${AGENT_IMAGE:-rancher/agent}

if [ -x "$(which boot2docker)" ]; then
    DOCKER_ARGS=${DOCKER_ARGS:--e CATTLE_AGENT_IP=$(boot2docker ip)}
fi

# This is just here to make sure your environment is sane
docker info

CONSOLE_ARGS=""
if [ -t 1 ]; then
    CONSOLE_ARGS="-it"
fi

HOST=${1:-http://${DEV_HOST}}
docker run $DOCKER_ARGS --rm $CONSOLE_ARGS -v /var/run/docker.sock:/var/run/docker.sock $AGENT_IMAGE $HOST
