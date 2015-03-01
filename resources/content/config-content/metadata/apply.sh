#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

bash set-ip.sh

if [ -e /etc/init.d/cattle-node ]; then
    /etc/init.d/cattle-node reload
fi

stage_files
