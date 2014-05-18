#!/bin/bash
set -e

# Need to fill these in
CATTLE_ACCESS_KEY=registrationToken \
CATTLE_SECRET_KEY=6A44B948DA598995BE5F:1400184000000:827816d4e01d17896bd899b47ea757c356d3aaf9 \
CATTLE_URL=http://192.168.3.143:8080/v1 \

docker build -t agent-register .
docker run \
    --rm \
    -e CATTLE_ACCESS_KEY=$CATTLE_ACCESS_KEY \
    -e CATTLE_SECRET_KEY=${CATTLE_SECRET_KEY} \
    -e CATTLE_URL=${CATTLE_URL} \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /var:/host/var \
    -t -i agent-register "$@"
