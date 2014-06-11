#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

chmod 600 content/etc/monit/monitrc

stage_files

if ! grep -q monit /etc/inittab; then
    echo '::respawn:/usr/bin/monit -Ic /etc/monit/monitrc' >> /etc/inittab
    kill -HUP 1
fi
