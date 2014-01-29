#!/bin/bash

cd $(dirname $0)
if ! docker images | grep -q ibuildthecloud/dstack-buildenv
then
    ./build-env.sh
fi
cd ../..
mkdir -p dist/artifacts
docker run -v $(pwd):/root -t -i ibuildthecloud/dstack-buildenv /root/dstack.sh build
