#!/bin/bash

cd $(dirname $0)

if [ ! -e nsenter ]; then
    cd nsenter-build
    docker build -t nsenter .
    cd ..
    C=$(docker run -d nsenter bash)
    # No clue why docker throws an error here, but it works
    docker cp ${C}:nsenter . || true
    if [ ! -e nsenter ]; then
        echo "Failed to copy"
        exit 1
    fi
fi

docker build -t cattle/agent .
