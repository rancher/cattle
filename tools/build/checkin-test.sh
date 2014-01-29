#!/bin/bash
set -e

cd $(dirname $0)
pushd ../../
rm -rf runtime
find . -depth -type d -name .tox -exec rm -rf {} \;
mvn clean
popd
./build.sh
TOXARGS="-e flake8,py27" ./runtests.sh
