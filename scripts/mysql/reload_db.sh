#!/bin/bash

cd $(dirname $0)
./setup_db.sh
./drop_tables.sh
mysql -u root cattle < mysql-dump.sql
