#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

get_config services

for i in $(cat ${CATTLE_HOME}/services | grep -vE '^(services|agent-instance-startup|configscripts)$'); do
    echo Getting $i
    get_config $i
done
