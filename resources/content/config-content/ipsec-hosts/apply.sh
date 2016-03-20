#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

if [ "$CATTLE_AGENT_STARTUP" != "true" ] && [ "$(pidof rancher-net)" != "" ]; then
    APPLIED=false
    for ((i=0; i<3 ;i++ )); do
        if curl --connect-timeout 5 -sf http://localhost:8111/v1/reload; then
            APPLIED=true
            break
        fi
        sleep 1
    done

    if [ "$APPLIED" != "true" ]; then
        curl --connect-timeout 5 -s http://localhost:8111/v1/reload
        echo Failed to reload ipsec config
    fi
fi
