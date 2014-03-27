#!/bin/bash
set -e

trap cleanup EXIT

cleanup()
{
    if [ -e test-requirements.txt.save ]; then
        mv test-requirements.txt.save test-requirements.txt
    fi
}

TOX_DEPS=()
BASE_DIR=$(dirname $(readlink -f $0))

for i in $BASE_DIR/runtests-agent-before.d/*; do
    if [ -x $i ]; then
        $i
    fi
done

cd $BASE_DIR/../../code/agent/src/agents/pyagent

pwd
for i in $BASE_DIR/*before.d/*-agent-env; do
    if [ -e $i ]; then
        source $i
    fi
done

for i in "${TOX_DEPS[@]}"; do
    if [ ! -e test-requirements.txt.save ]; then
        cp test-requirements.txt test-requirements.txt.save
    fi
    echo $i >> test-requirements.txt
done

tox ${TOXARGS:--e py26,py27}
