#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

# exec script to execute swarm
if [ -f /var/run/swarm.pid ]
then
    /etc/init.d/cluster stop
    /etc/init.d/cluster start
else
    /etc/init.d/cluster start
fi

