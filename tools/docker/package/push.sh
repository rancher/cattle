#!/bin/bash

cd $(dirname $0)

if [ -e images ]; then
    for i in $(<images); do
        docker push $i
        if [ "$1" == "--latest" ] || [ "$1" == "-l" ]; then
            LATEST_TAG="$(echo $i | sed 's/:.*/:latest/g')"
            docker tag $i $LATEST_TAG
            docker push $LATEST_TAG
        fi
    done
fi
