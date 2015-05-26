#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

# exec script to execute socat
if [ -f /var/run/ssl-socat.pid ]
then
    /etc/init.d/ssl-socat stop
    /etc/init.d/ssl-socat start
else
    /etc/init.d/ssl-socat start
fi

