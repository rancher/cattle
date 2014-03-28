#!/bin/bash
set -e

export CATTLE_HOME=${CATTLE_HOME:-/var/lib/cattle}

install()
{
    cd $(dirname $0)
    if [ -e $CATTLE_HOME/common ]; then
        rm -rf $CATTLE_HOME/common
    fi

    cp -rf common $CATTLE_HOME/common
    cp config.sh $CATTLE_HOME/config.sh
    chmod +x $CATTLE_HOME/config.sh
}

install
