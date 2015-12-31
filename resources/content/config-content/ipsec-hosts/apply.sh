#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

if [ "$CATTLE_AGENT_STARTUP" != "true" ]; then
    curl -f http://localhost:8111/v1/reload || {
        echo Failed to reload ipsec config
        curl http://localhost:8111/v1/reload {
        exit 1
    }
fi
