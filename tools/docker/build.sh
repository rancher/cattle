#!/bin/bash

cd $(dirname $0)/../..

mkdir -p dist/artifacts

CMD="docker run --rm -e BUILD_USER_ID=$(id -u) -e DSTACK_DB_DSTACK_DATABASE=$FORCE_DB -v $(pwd):/root -w /root -t -i"

$CMD dstack-buildenv ./dstack.sh "$@"
