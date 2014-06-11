#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

if /etc/init.d/cattle-node status; then
    /etc/init.d/cattle-node restart
else
    /etc/init.d/cattle-node start
fi
