#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

chmod 600 content/etc/monitrc

stage_files

if ! grep -q monit /etc/inittab; then
    echo '::respawn:/usr/bin/monit -Ic /etc/monitrc' >> /etc/inittab
    if [ "$CATTLE_AGENT_STARTUP" != "true" ]; then
        kill -1 1
    fi
fi
