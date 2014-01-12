#!/bin/bash
set -e

TAG=ibuildthecloud/dstack-buildenv

cd $(dirname $0)

if [ "$1" = "indocker" ]
then
    cd $(dirname $0)/../..
    mvn -DskipTests=true install
    mvn dependency:go-offline
    rm -rf ~/.m2/repository/$(echo io.github.ibuildthecloud | sed 's/\./\//g')
    rm -rf /usr/src/dstack
    exit 0
fi

rm -rf build
mkdir -p build
tar cf - --exclude .tox --exclude .git --exclude tools/docker/build -C ../.. . | tar xvf - -C build
cp Dockerfile build
cd build
docker build -t $TAG . 2>&1 | tee ../docker-build.log
