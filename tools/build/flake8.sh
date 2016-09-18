#!/bin/bash
set -e

cd $(dirname $0)
cd ../../tests/integration
tox -e flake8
cd ../integration-v1
tox -e flake8
