#!/bin/bash
set -e

cd $(dirname $0)

mysql -u root < create_db_and_user_dev.sql
