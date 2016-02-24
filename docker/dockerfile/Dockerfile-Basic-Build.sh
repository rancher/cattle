#!/bin/bash
set -e -x

cd $(dirname $0)

docker build -f Dockerfile-Basic -t dstack/ubuntu .