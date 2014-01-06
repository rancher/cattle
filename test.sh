#!/bin/bash
set -e

PORT=8080

checkPort()
{  
    netstat -an | grep -q ':'${PORT}'.*LISTEN'
}

if ! checkPort
then
    ./run.sh &
    LASTPID=$!
fi

for ((i=0;i<600;i++))
do
    if checkPort
    then
        break
    else
        echo "Waiting for start"
        sleep 1
    fi
done

cd integration
. ./env
tox

if [ "$LASTPID" != "" ]
then
    kill $LASTPID
fi
