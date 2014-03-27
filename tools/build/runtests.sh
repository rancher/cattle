#!/bin/bash
set -e -x

if [ -z "$TEST_INCEPTION" ]; then
    # Work around issue https://github.com/dotcloud/docker/issues/4854
    export TEST_INCEPTION=true
    $0 "$@"
fi

trap cleanup EXIT

BASE_DIR=$(dirname $(readlink -f $0))

cleanup()
{
    if [ "$LASTPID" != "" ]; then
        kill $LASTPID
    fi
}

before()
{
    for i in $BASE_DIR/runtests-before.d/*; do
        if [ -x $i ]; then
            $i
        fi
    done

    for i in $BASE_DIR/*before.d/*-integration-env; do
        if [ -e $i ]; then
            source $i
        fi
    done
}

PORT=8080

cd $(dirname $0)/../..

checkPort()
{  
    netstat -an | grep -q ':'${PORT}'.*LISTEN'
}

if ! checkPort; then
    export DSTACK_LOGBACK_ROOT_LEVEL=WARN
    ./dstack.sh run &
    LASTPID=$!
fi

for ((i=0;i<600;i++))
do
    if checkPort; then
        break
    else
        echo "Waiting for start"
        sleep 1
    fi
done

if ! checkPort; then
    echo "Server did not start"
    exit 1
fi

before

(
    cd tests/integration
    . ./env
    tox $TOXARGS
)

if [ "$EXTERNAL_AGENT_TEST" != "true" ]; then
    TOXARGS='-e flake8,py27' ./tools/build/runtests-agent.sh
fi
