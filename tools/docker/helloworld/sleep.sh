#!/bin/sh

COUNT=${COUNT:-120}

for i in `seq 1 $COUNT`; do
    echo Sleeping ${i}/${COUNT}
    sleep 1
done
