#!/bin/bash

trap exit SIGINT

SCRIPT=/tmp/bootstrap.sh

while true; do
    curl -u ${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY} -s ${CATTLE_URL}/scripts/bootstrap > $SCRIPT 
    chmod +x $SCRIPT

    echo "echo INFO: Starting for agent ${CATTLE_ACCESS_KEY}" | bash $SCRIPT

    sleep 15
done
