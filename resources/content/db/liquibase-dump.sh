#!/bin/bash
set -x -e

LIQUIBASE_HOME=${LIQUIBASE_HOME:-~/.local/liquibase}
DB=${DB:-dstack}
DRIVER=${DRIVER:-"$HOME/.m2/repository/mysql/mysql-connector-java/5.1.26/mysql-connector-java-5.1.26.jar"}

$LIQUIBASE_HOME/liquibase --classpath="$DRIVER" --driver=com.mysql.jdbc.Driver --changeLogFile=dump.xml --url="jdbc:mysql://localhost:3306/$DB" --username=$DB --password=$DB generateChangeLog
