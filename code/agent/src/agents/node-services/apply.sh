#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

# Make sure that when node start is doesn't think it holds the config.sh lock
unset CATTLE_CONFIG_FLOCKER

if /etc/init.d/cattle-node status; then
    /etc/init.d/cattle-node restart
else
    /etc/init.d/cattle-node start
fi
