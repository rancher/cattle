#!/bin/bash
set -e

source ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

cd $(dirname $0)

stage()
{
    mapfile PACKAGES < packages

    for i in "${PACKAGES[@]}"; do
        set $i
        URL=$2

        if [ "$URL" = "config" ]; then
            ${CATTLE_HOME}/config.sh $1
        else
            ${CATTLE_HOME}/config.sh --archive-url $URL $1
        fi
    done
}

stage
