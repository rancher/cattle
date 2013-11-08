#!/bin/bash
set -e

cd $(dirname $0)
/usr/sbin/mysqld &
while true
do
  if /usr/bin/mysqladmin ping 2>/dev/null
  then
    break
  else
    sleep 1
  fi
done
mysql -u root -py < ../resources/content/db/mysql/create_db_and_user_dev.sql
cd ..
cat > resources/content/modules.properties << EOF
module.excludes=core
EOF
bash -x ./run.sh
