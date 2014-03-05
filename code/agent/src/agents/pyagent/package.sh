#!/bin/bash

cd $(dirname $0)
VER=$(pip --version | awk '{print $2}')
MAJOR=$(echo $VER | cut -f1 -d.)
MINOR=$(echo $VER | cut -f2 -d.)
if [ $MAJOR -lt 2 ] && [ $MINOR -lt 5 ]
then
    echo "[ERROR] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 1>&2
    echo "[ERROR] !! pip 1.5 or newer is required !!" 1>&2
    echo "[ERROR] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 1>&2
    exit 1
fi
if [ ! -e dist ]; then
    pip install -t dist -r requirements.txt
fi
