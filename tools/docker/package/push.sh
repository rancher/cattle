#!/bin/bash

cd $(dirname $0)

if [ -e images ]; then
    for i in $(<images); do
        docker push $i
    done
fi
