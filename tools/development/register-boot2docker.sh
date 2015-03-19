#!/bin/bash
set -e -x

DEV_HOST=${DEV_HOST:-10.0.2.2:8080}

if [ -x "$(which boot2docker)" ]; then
    DOCKER_ARGS=${DOCKER_ARGS:--e CATTLE_AGENT_IP=$(boot2docker ip)}
fi

# This is just here to make sure your environment is sane
docker info

CONSOLE_ARGS=""
if [ -t 1 ]; then
    CONSOLE_ARGS="-it"
fi

# Set the api.host setting
curl -F "name=api.host" \
     -F "type=activeSetting" \
     -F "value=$DEV_HOST" \
     http://localhost:8080/v1/settings

# Wait for setting to become active. Command from registration token won't be right until it is.
API_HOST_LINK=$(curl -H 'Content-Type: application/json' -X GET 'http://localhost:8080/v1/settings/api.host' | jq -r '.links.self')
API_HOST_VALUE=""
while [ "$API_HOST_VALUE" != "$DEV_HOST" ]; do
    echo "Waiting for api.host setting to become active."
    sleep 5
    API_HOST_VALUE=$(curl -H 'Content-Type: application/json' "$API_HOST_LINK" | jq -r '.activeValue')
done

# Create a registration token and wait for it to become active
REG_TOKEN_LINK=$(curl -H 'Content-Type: application/json' -X POST http://localhost:8080/v1/registrationtoken | jq -r '.links.self')
TRANSITIONING="yes"
while [ "$TRANSITIONING" == "yes" ]; do
    echo "Waiting for registration token to become active."
    sleep 1
    TRANSITIONING=$(curl -H 'Content-Type: application/json' "$REG_TOKEN_LINK" | jq '.transitioning')
done

# Extract the command from the registration token
COMMAND=$(curl -H 'Content-Type: application/json' "$REG_TOKEN_LINK" | jq -r '.command')

# Remove sudo. Not needed in a typical b2d setup
COMMAND=${COMMAND/sudo docker run/docker run $CONSOLE_ARGS $DOCKER_ARGS}

$COMMAND
