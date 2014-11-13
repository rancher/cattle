#!/bin/bash

cd $(dirname $0)/../..

CMD="docker run -e BUILD_USER_ID=$(id -u) -e CATTLE_DB_CATTLE_DATABASE=$FORCE_DB -v $(pwd):/root -w /root -t -i"

if [ "$1" = "run" ]; then
    CMD="$CMD -p ${CATTLE_HTTP_PORT:-8080}:8080"
fi

$CMD cattle-buildenv ./cattle.sh "$@"
