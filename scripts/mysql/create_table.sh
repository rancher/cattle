#!/bin/bash

cd $(dirname $0)

if [ "$#" != 1 ]; then
    echo Usage: $0 TABLE_NAME
    exit 1
fi

. tables-common.sh

default_table $1 | mysql -u root cattle
