#!/bin/bash

cd $(dirname $0)

if [ "$CATTLE_DB_CATTLE_DATABASE" = "mysql" ]; then
    echo Creating cattle database and user
    mysql -u root < ../../../resources/content/db/mysql/create_db_and_user_dev.sql
fi
