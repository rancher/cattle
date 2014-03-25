#!/bin/bash
set -x -e

LIQUIBASE_HOME=${LIQUIBASE_HOME:-~/.local/liquibase}
DB=${DB:-dstack}
DRIVER=${DRIVER:-"$HOME/.m2/repository/mysql/mysql-connector-java/5.1.26/mysql-connector-java-5.1.26.jar"}

if [ -e dump.xml ]; then
    mv dump.xml dump-$(date '+%s').xml
fi

$LIQUIBASE_HOME/liquibase --classpath="$DRIVER" --driver=com.mysql.jdbc.Driver --changeLogFile=dump.xml --url="jdbc:mysql://localhost:3306/$DB" --username=$DB --password=$DB generateChangeLog

sed -i -E \
    -e 's/id="[0-9]+-([0-9]+)/id="dump\1/g' \
    -e 's/BIGINT\(19\)/BIGINT/g' \
    -e 's/BIT\(1\)/BIT/g' \
    -e 's/INT\(10\)/INT/g' \
    dump.xml
