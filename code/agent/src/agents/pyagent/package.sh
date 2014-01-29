#!/bin/bash

cd $(dirname $0)
if [ ! -e dist ]; then
    pip install -t dist -r requirements.txt
fi
