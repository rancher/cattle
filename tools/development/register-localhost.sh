#!/bin/bash

DOCKER_IP=$(ip addr show dev docker0 | grep 'inet ' | awk '{print $2}' | cut -f1 -d'/')
echo DOCKER_ARGS="-e CATTLE_AGENT_IP=127.0.0.1" DEV_HOST=${DOCKER_IP}:8080 $(dirname $0)/register-boot2docker.sh
DOCKER_ARGS="-e CATTLE_AGENT_IP=127.0.0.1" DEV_HOST=${DOCKER_IP}:8080 $(dirname $0)/register-boot2docker.sh
