#!/bin/bash
set -e

CATTLE_HOME=${CATTLE_HOME:-/var/lib/cattle}
CATTLE_AGENT_IMAGE=${CATTLE_AGENT_IMAGE:-cattle/agent:latest}
HOST_TOKEN_FILE=/var/lib/cattle/.registration_token
TOKEN_FILE=${TOKEN_FILE:-/host${HOST_TOKEN_FILE}}
TOKEN=

mkdir -p /host/${CATTLE_HOME}

if [ -e $TOKEN_FILE ]; then
    TOKEN="$(<$TOKEN_FILE)"
fi

if [ -z "$TOKEN" ]; then
    TOKEN=$(openssl rand -hex 64)
    mkdir -p $(dirname $TOKEN_FILE)
    echo $TOKEN > $TOKEN_FILE

    echo "Created $HOST_TOKEN_FILE, do not delete it.  It identifies this server with the core system."
fi

NAME="agent-${TOKEN}"

if docker inspect $NAME 1>/dev/null 2>&1; then
    #echo "Removing existing container $NAME"
    #docker kill $NAME
    #docker rm -f $NAME
    echo "Starting existing container $NAME"
    docker start $NAME >/dev/null
    ID=$(docker inspect --format '{{.ID}}' $NAME)
    exec docker logs -f $ID
fi


echo "Creating container $NAME"

ENV=$(./register.py $TOKEN)
eval "$ENV"

ID=$(docker run \
    -e CATTLE_URL="$CATTLE_URL" \
    -e CATTLE_CONFIG_URL="${CATTLE_CONFIG_URL:-${CATTLE_URL}}" \
    -e CATTLE_STORAGE_URL="${CATTLE_STORAGE_URL:-${CATTLE_URL}}" \
    -e CATTLE_AGENT_IP="$CATTLE_AGENT_IP" \
    -e CATTLE_ACCESS_KEY="$CATTLE_ACCESS_KEY" \
    -e CATTLE_SECRET_KEY="$CATTLE_SECRET_KEY" \
    -e CATTLE_SCRIPT_DEBUG=$CATTLE_SCRIPT_DEBUG \
    -e INCEPTION=true \
    -v ${CATTLE_HOME}:${CATTLE_HOME} \
    -v /proc:/host/proc \
    -v /var:/host/var \
    -v /run:/host/run \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /var/lib/docker:/var/lib/docker \
    --privileged \
    --name $NAME \
    -d \
    $CATTLE_AGENT_IMAGE)

exec docker logs -f $ID
