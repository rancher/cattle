#!/bin/bash
set -e

export DSTACK_HOME=${DSTACK_HOME:-/var/lib/dstack}

install()
{
    cd $(dirname $0)
    if [ -e $DSTACK_HOME/common ]; then
        rm -rf $DSTACK_HOME/common
    fi

    cp -rf common $DSTACK_HOME/common
    cp config.sh $DSTACK_HOME/config.sh
    chmod +x $DSTACK_HOME/config.sh
}

install
