#!/bin/bash

cd $(dirname $0)

TS=$(date '+%s')
mysqldump -u root cattle | gzip -c > cattle_dump_${TS}.sql.gz
mysql -u root < drop_tables.sql
