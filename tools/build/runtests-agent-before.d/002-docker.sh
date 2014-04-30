#!/bin/bash

if [ "$DOCKER_TEST" = "true" ]; then
    docker pull cattle/agent-instance:latest
fi
