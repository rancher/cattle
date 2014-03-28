#!/bin/bash

cd $(dirname $0)

if [ "$CATTLE_DB_CATTLE_DATABASE" = "postgres" ]
then
    echo Creating cattle database and user
    psql -U postgres -c "CREATE USER cattle SUPERUSER PASSWORD 'cattle'"
    psql -U postgres -c "CREATE DATABASE cattle"
fi
