#!/bin/bash

cd $(dirname $0)
./setup_db.sh
./drop_tables_base.sh
mysql -u root cattle_base < mysql-dump.sql
