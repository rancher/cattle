#!/bin/bash

cd $(dirname $0)

TS=$(date '+%s')
mysqldump -u root cattle > cattle_dump_${TS}.sql
mysql -u root < drop_tables.sql
