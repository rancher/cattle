#!/bin/bash
# This is a dumb script to pull docker git every hour and run master
set -e

trap cleanup EXIT

cleanup()
{
    if [ -n "$PID" ]; then
        kill $PID
    fi

    chown -R darren .
}

if [ "$1" = "" ]; then
    echo "Please specify the directory where you want to clone docker"
    exit 1
fi

mkdir -p $1
cd $1

DIR="$1/docker"

if [ ! -e "$DIR" ]; then
    git clone https://github.com/dotcloud/docker.git
fi

cd $DIR

while true; do
    start docker || true
    git pull -r
    rm -rf bundles
    make
    stop docker || true
    ./bundles/*/binary/*-dev -d -D &
    PID=$!
    sleep 3600
    cleanup
done
