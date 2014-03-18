#!/bin/bash

if [ "$DOCKER_TEST" = "true" ]; then
    echo 'export DOCKER_TEST=true' > ${0}-agent-env
fi
