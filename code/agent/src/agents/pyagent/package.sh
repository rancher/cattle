#!/bin/bash

cd $(dirname $0)

if diff -q dist-requirements.txt dist/dist-requirements.txt >/dev/null 2>&1; then
    exit 0
fi

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

if [ -e dist ]; then
    rm -rf dist
fi

pip install -t dist -r dist-requirements.txt
cp dist-requirements.txt dist
