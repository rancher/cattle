#!/bin/bash
set -e

export TAG=${TAG:-latest}

docker pull cattle/agent:${TAG}

cd agent
./build.sh

for i in instance register; do
    cd ../agent-${i}
    docker pull cattle/agent-${i}:${TAG}
    docker build -t cattle/agent-${i}:${TAG} .
done
