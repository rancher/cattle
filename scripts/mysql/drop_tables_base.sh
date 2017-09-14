#!/bin/bash

cd $(dirname $0)

TS=$(date '+%s')
mysqldump -u root cattle_base > cattle_dump_base_${TS}.sql
mysql -u root < drop_tables_base.sql
