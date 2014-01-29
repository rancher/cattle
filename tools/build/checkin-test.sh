#!/bin/bash
set -e

cd $(dirname $0)
pushd ../../
rm -rf runtime
mvn clean
popd
./build.sh
TOXARGS="-e py27,flake8" ./runtests.sh
