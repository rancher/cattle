#!/bin/bash

mysqldump -u root dstack > dstack_dump_$(date '+%s').sql
mysql -u root < reload_db.sql
