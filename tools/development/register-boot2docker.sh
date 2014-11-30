#!/bin/bash
set -e

DEV_HOST=${DEV_HOST:-10.0.2.2:8080}

# This is just here to make sure your environment is sane
docker info

docker run --rm -it -v /var/run/docker.sock:/var/run/docker.sock rancher/agent http://${DEV_HOST}
