#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

stage_files

touch $CATTLE_HOME/.pyagent-stamp
