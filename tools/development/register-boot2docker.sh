#!/bin/bash
set -e

DEV_HOST=${DEV_HOST:-10.0.2.2:8080}
AGENT_IMAGE=${AGENT_IMAGE:-rancher/agent}

# This is just here to make sure your environment is sane
docker info

CONSOLE_ARGS=""
if [ -t 1 ]; then
    CONSOLE_ARGS="-it"
fi

docker run $DOCKER_ARGS --rm $CONSOLE_ARGS -v /var/run/docker.sock:/var/run/docker.sock $AGENT_IMAGE http://${DEV_HOST}
