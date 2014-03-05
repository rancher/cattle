#!/bin/bash
set -e

trap cleanup EXIT

cleanup()
{
    if [ "$LASTPID" != "" ]
    then
        kill $LASTPID
    fi
}

PORT=8080

cd $(dirname $0)/../..

checkPort()
{  
    netstat -an | grep -q ':'${PORT}'.*LISTEN'
}

if ! checkPort
then
    export DSTACK_LOGBACK_ROOT_LEVEL=WARN
    ./dstack.sh run &
    LASTPID=$!
fi

for ((i=0;i<600;i++))
do
    if checkPort
    then
        break
    else
        echo "Waiting for start"
        sleep 1
    fi
done

if ! checkPort
then
    echo "Server did not start"
    exit 1
fi

cd tests/integration
. ./env
tox $TOXARGS
cd ../../code/agent/src/agents/pyagent
tox $TOXARGS
