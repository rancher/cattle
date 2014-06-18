#!/bin/bash
set -e

trap cleanup EXIT

cleanup()
{
    local exit=$?

    if [ -e $TEMP ]; then
        rm -rf $TEMP
    fi
    if [ -e $OLD ]; then
        rm -rf $OLD
    fi

    return $exit
}

source ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

DEST=$CATTLE_HOME/pyagent
MAIN=$DEST/main.py
STAMP=$CATTLE_HOME/.pyagent-stamp
OLD=$(mktemp -d ${DEST}.XXXXXXXX)
TEMP=$(mktemp -d ${DEST}.XXXXXXXX)

cd $(dirname $0)

stage()
{
    cp -rf apply.sh cattle dist main.py $TEMP

    find $TEMP -name "*.sh" -exec chmod +x {} \;
    find $TEMP \( -name nsenter -o -name socat \) -exec chmod +x {} \;

    if [ -e $DEST ]; then
        mv $DEST ${OLD}
    fi
    mv $TEMP ${DEST}
    rm -rf ${OLD}

    echo $RANDOM > $STAMP
}

conf()
{
    CONF=(${CATTLE_HOME}/pyagent/agent.conf
          /etc/cattle/agent/agent.conf
          ${CATTLE_HOME}/etc/cattle/agent/agent.conf)

    for conf_file in "${CONF[@]}"; do
        if [ -e $conf_file ]
        then
            source $conf_file
        fi
    done
}

start()
{
    chmod +x $MAIN
    if [ "$CATTLE_PYPY" = "true" ] && which pypy >/dev/null; then
        MAIN="pypy $MAIN"
    fi

    info Executing $MAIN
    cleanup
    exec $MAIN
}

conf

if [ "$1" = "start" ]; then
    start
else
    stage
fi
