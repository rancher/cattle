#!/bin/bash
set -e

TAG=ibuildthecloud/dstack-buildenv

cd $(dirname $0)

docker build -t $TAG . 2>&1 | tee docker-build.log
