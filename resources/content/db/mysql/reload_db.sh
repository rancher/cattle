#!/bin/bash

cd $(dirname $0)
./drop_tables.sh
mysql -u root cattle < mysql-dump.sql
