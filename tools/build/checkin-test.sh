#!/bin/bash
set -e

cd $(dirname $0)

if test -x $(which flake8); then
    ./flake8.sh
fi

cd ../..

CHECK_DIR=$(pwd)-check

if [ -d $CHECK_DIR ]
then
    mkdir -p $CHECK_DIR
fi

rsync -av --exclude '*.log' --exclude '*.gz' --delete . ${CHECK_DIR}/

cd $CHECK_DIR

find -depth -type d -name __pycache__ -exec rm -rf {} \;
if [ -e resources/content/cattle-local.properties ]
then
    rm resources/content/cattle-local.properties
fi

find . -depth -type d -name .tox -exec rm -rf {} \;

pwd

if [ "$1" = "release" ]; then
    git reset --hard HEAD
    git clean -dxf
    make release-docker
else
    make clean test "$@"
fi
