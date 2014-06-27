#!/bin/bash

trap "" SIGHUP
trap kill_and_exit EXIT

PGID=$(echo $(ps -p $$ o pgid h))
KILL=false

kill_and_exit()
{
    local ret=$?
    echo Caught EXIT
    do_kill -9
    exit $?
}

print_ps()
{
    ps -eO pgid | grep -E 'PGID|'" $PGID"
}

do_kill()
{
    print_ps
    PIDS=
    for p in $(ps -e h o pid,pgid | grep -E ' +'${PGID}'$' | awk '{print $1}' | grep -v $$); do
        if [ -e /proc/$p ]; then
            PIDS="$PIDS $p"
        fi
    done

    if [ -n "$PIDS" ]; then
        print_ps
        echo PID $PGID has died doing kill "$@" $PIDS
        kill "$@" $PIDS
    fi
}

main()
{
    while sleep 2; do
        if [ -e /proc/${PGID} ]; then
            #print_ps
            continue
        fi

        if [ "$KILL" = "false" ]; then
            do_kill
            KILL=true
        else
            do_kill -9
            break
        fi
    done
}

main
