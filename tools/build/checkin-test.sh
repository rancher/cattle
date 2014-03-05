#!/bin/bash
set -e

cd $(dirname $0)

./flake8.sh

cd ../..

CHECK_DIR=$(pwd)-check

if [ -d $CHECK_DIR ]
then
    mkdir -p $CHECK_DIR
fi

rsync -av --exclude '*.log' --exclude '*.gz' --delete . ${CHECK_DIR}/

cd ${CHECK_DIR}/tools/build
pushd ../..

find -depth -type d -name __pycache__ -exec rm -rf {} \;
if [ -e resources/content/dstack-local.properties ]
then
    rm resources/content/dstack-local.properties
fi

rm -rf runtime
find . -depth -type d -name .tox -exec rm -rf {} \;
mvn clean

popd

./build.sh
TOXARGS="-e flake8,py27" ./runtests.sh
