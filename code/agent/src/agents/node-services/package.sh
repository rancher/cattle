#!/bin/bash

cd $(dirname $0)/content-home/node-services
if [ ! -e node_modules ]; then
    npm install
fi
