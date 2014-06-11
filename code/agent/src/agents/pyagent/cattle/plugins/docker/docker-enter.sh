#!/bin/bash

sudo $(dirname $0)/nsenter -m -u -i -n -p -t $(docker inspect --format '{{.State.Pid}}' $1)
