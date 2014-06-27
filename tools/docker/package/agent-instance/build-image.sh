#!/bin/bash

TAG=${TAG:-dev}
IMAGE=cattle/agent-instance:${TAG}

cd $(dirname $0)
docker build -t ${IMAGE} .

if [ -n "$IMAGE_REGISTRY" ]; then
    echo $IMAGE >> ${IMAGE_REGISTRY}
fi
