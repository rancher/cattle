#!/bin/bash

cd $(dirname $0)

for i in before.d/*; do
    if [ -x $i ]; then
        ./$i
    fi
done
