#!/bin/bash
set -x -e

LIQUIBASE_HOME=${LIQUIBASE_HOME:-~/.local/liquibase}
DB=${DB:-cattle}
DRIVER_VERSION=5.1.34

function prep_driver_jar(){
    if [ -n "$DRIVER" ]; then
        echo $DRIVER
    else
        if [ -z "$(find $HOME/.m2 -name mysql-connector-java*.jar)" ]; then
            mvn -DgroupId=mysql -DartifactId=mysql-connector-java -Dversion=$DRIVER_VERSION dependency:get &> /dev/null
        fi
        if [ -n "$(find $HOME/.m2 -name mysql-connector-java*.jar)" ]; then
            echo $(find $HOME/.m2 -name mysql-connector-java*.jar -print -quit)
        else
            # Couldn't install driver
            return 1
        fi
    fi
}

if [ -e dump.xml ]; then
    mv dump.xml dump-$(date '+%s').xml
fi

DRIVER_JAR=$(prep_driver_jar)

JAVA_OPTS="-Duser.name=rancher" $LIQUIBASE_HOME/liquibase --classpath="$DRIVER_JAR"  \
    --driver=com.mysql.jdbc.Driver \
    --changeLogFile=dump.xml \
    --url="jdbc:mysql://localhost:3306/${DB}_base" \
    --username=$DB \
    --password=$DB \
     diffChangeLog \
    --referenceUrl="jdbc:mysql://localhost:3306/$DB" \
    --referenceUsername=$DB \
    --referencePassword=$DB

sed -i -E \
    -e '2r header.xml' \
    -e 's/id="[0-9]+-([0-9]+)/id="dump\1/g' \
    -e 's/BIGINT\(19\)/BIGINT/g' \
    -e 's/BIT\(1\)/BIT/g' \
    -e 's/INT\(10\)/INT/g' \
    dump.xml

cat dump.xml
