#!/bin/bash

cd $(dirname $0)

if [ "$DSTACK_DB_DSTACK_DATABASE" = "postgres" ]
then
    echo Creating dstack database and user
    psql -U postgres -c "CREATE USER dstack SUPERUSER PASSWORD 'dstack'"
    psql -U postgres -c "CREATE DATABASE dstack"
fi
