#!/bin/bash
set -e

check_dev()
{
    ip link show dev $DEV >/dev/null 2>&1
}

wait_for_dev()
{
    for i in {1..100}; do
        if check_dev; then
            return
        else
            sleep .1
        fi
    done
    if ! check_dev; then
        echo "$DEV does not exist" 1>&2
        exit 1
    fi
}

IP=
MAC=
PID=
DEV=eth0
ARGS="$@"

while [ "$#" -gt 0 ]; do
    case $1 in
    -p)
        shift 1
        PID=$1
        ;;
    -m)
        shift 1
        MAC=$1
        ;;
    -i)
        shift 1
        IP=$1
        ;;
    -d)
        shift 1
        DEV=$1
        ;;
    esac
    shift 1
done

if [ "$(id -u)" != 0 ]; then
    echo "Not root, not executing $ARGS"
    exit 0
fi

if [[ "$PID" = "" || ! -e /proc/$PID/ns/net ]]; then
    echo "Invalid PID $PID"
    exit 1
fi

if [ "$_ENTERED" != "true" ]; then
    NSENTER=nsenter
    if [ ! -x "$(which nsenter)" ]; then
        NSENTER=$(dirname $0)/nsenter
    fi
    _ENTERED=true exec $NSENTER -n -t $PID -F -- $0 $ARGS

    # Not possible
    exit 0
fi

wait_for_dev
ip link show
ip addr show

if [ -n "$IP" ]; then
    if ! echo $IP | grep -q /; then
        # Just assume you really wanted a /24
        IP=${IP}/24
    fi

    if ! ip addr show dev $DEV  | grep '[[:space:]]*inet' | awk '{ print $2 }' | grep -q $IP; then
        echo "Adding $IP to $DEV"
        ip addr add dev $DEV $IP
        ip link show
        ip addr show
    fi
fi

if [ -n "$MAC" ]; then
    if ! ip link show dev $DEV | grep -qi $MAC; then
        echo "Setting $DEV to $MAC"
        ip link set dev $DEV address $MAC
    fi
fi
