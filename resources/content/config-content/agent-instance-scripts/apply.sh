#!/bin/bash

. ${CATTLE_HOME:-/var/lib/cattle}/common/scripts.sh

chmod +x content-home/events/*

stage_files
