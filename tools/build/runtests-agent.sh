#!/bin/bash
set -e

TOX_DEPS=()
BASE_DIR=$(dirname $0)

cd $BASE_DIR/../../code/agent/src/agents/pyagent

for i in $BASE_DIR/*before.d/*-agent-env; do
    if [ -e $i ]; then
        source $i
    fi
done

for i in "${TOX_DEPS[@]}"; do
    echo $i >> test-requirements.txt
done

tox ${TOXARGS:--e py26,py27}
