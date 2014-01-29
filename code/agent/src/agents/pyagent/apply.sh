#!/bin/bash
set -e

trap cleanup EXIT

cleanup()
{
    if [ -e $TEMP ]; then
        rm -rf $TEMP
    fi
    if [ -e $OLD ]; then
        rm -rf $OLD
    fi
}

source ${DSTACK_HOME:-/var/lib/dstack}/common/scripts.sh

DEST=$DSTACK_HOME/pyagent
MAIN=$DEST/main.py
RESTART=$DEST/reboot
OLD=$(mktemp -d ${DEST}.XXXXXXXX)
TEMP=$(mktemp -d ${DEST}.XXXXXXXX)

cd $(dirname $0)

stage()
{
    cp -rf apply.sh dstack dist main.py $TEMP

    if [ -e $DEST ]; then
        mv $DEST ${OLD}
    fi
    mv $TEMP ${DEST}
    rm -rf ${OLD}
}

start()
{
    if [ -n "$NO_START" ]; then
        return 0
    fi
    echo $RANDOM > $RESTART
    chmod +x $MAIN
    if [ "$DAEMON" = false ]; then
        info Executing $MAIN
        exec $MAIN
    else
        info Backgrounding $MAIN
        $MAIN &
    fi
}

while [ "$#" -gt 0 ]; do
    case $1 in
    --no-start)
        NO_START=true
        ;;
    --no-daemon)
        DAEMON=false
        ;;
    start)
        start
        exit 0
        ;;
    esac
    shift 1
done

stage
start
