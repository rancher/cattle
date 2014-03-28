#!/bin/bash

cd $(dirname $0)/../..

mkdir -p dist/artifacts

CMD="docker run --rm -e BUILD_USER_ID=$(id -u) -e CATTLE_DB_CATTLE_DATABASE=$FORCE_DB -v $(pwd):/root -w /root -t -i"

$CMD cattle-buildenv ./cattle.sh "$@"
