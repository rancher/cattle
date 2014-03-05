#!/bin/bash
set -e

cd $(dirname $0)
cd ../../tests/integration
tox -e flake8
cd ../../code/agent/src/agents/pyagent
tox -e flake8
