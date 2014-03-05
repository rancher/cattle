#!/bin/bash
set -e

cd /var/lib/dstack

JAR=dstack.jar

if [ "$URL" != "" ]
then
    echo Downloading $URL
    curl -s $URL > dstack-download.jar
    JAR=dstack-download.jar
fi

setup_gelf()
{
    # Setup GELF
    export DSTACK_LOGBACK_OUTPUT_GELF_HOST=${DSTACK_LOGBACK_OUTPUT_GELF_HOST:-$GELF_PORT_12201_UDP_ADDR}
    export DSTACK_LOGBACK_OUTPUT_GELF_PORT=${DSTACK_LOGBACK_OUTPUT_GELF_PORT:-$GELF_PORT_12201_UDP_PORT}
    if [ -n "$DSTACK_LOGBACK_OUTPUT_GELF_HOST" ]; then
        export DSTACK_LOGBACK_OUTPUT_GELF=${DSTACK_LOGBACK_OUTPUT_GELF:-true}
    fi
}

setup_mysql()
{
    export DSTACK_DB_DSTACK_MYSQL_HOST=${DSTACK_DB_DSTACK_MYSQL_HOST:-$MYSQL_PORT_3306_TCP_ADDR}
    export DSTACK_DB_DSTACK_MYSQL_PORT=${DSTACK_DB_DSTACK_MYSQL_PORT:-$MYSQL_PORT_3306_TCP_PORT}
    if [ -n "$DSTACK_DB_DSTACK_MYSQL_HOST" ]; then
        export DSTACK_DB_DSTACK_DATABASE=${DSTACK_DB_DSTACK_DATABASE:-mysql}
    fi
}

setup_redis()
{
    local hosts=""
    local i=1

    while [ -n "$(eval echo \$REDIS${i}_PORT_6379_TCP_ADDR)" ]; do
        local host="$(eval echo \$REDIS${i}_PORT_6379_TCP_ADDR:\$REDIS${i}_PORT_6379_TCP_PORT)"

        if [ -n "$hosts" ]; then
            hosts="$hosts,$host"
        else
            hosts="$host"
        fi

        i=$((i+1))
    done

    if [ -n "$hosts" ]; then
        export DSTACK_REDIS_HOSTS=${DSTACK_REDIS_HOSTS:-$hosts}
    fi

    if [ -n "$DSTACK_REDIS_HOSTS" ]; then
        export DSTACK_MODULE_PROFILE_REDIS=true
    fi
}

setup_zk()
{
    local hosts=""
    local i=1

    while [ -n "$(eval echo \$ZK${i}_PORT_2181_TCP_ADDR)" ]; do
        local host="$(eval echo \$ZK${i}_PORT_2181_TCP_ADDR:\$ZK${i}_PORT_2181_TCP_PORT)"

        if [ -n "$hosts" ]; then
            hosts="$hosts,$host"
        else
            hosts="$host"
        fi

        i=$((i+1))
    done

    if [ -n "$hosts" ]; then
        export DSTACK_ZOOKEEPER_CONNECTION_STRING=${DSTACK_ZOOKEEPER_CONNECTION_STRING:-$hosts}
    fi

    if [ -n "$DSTACK_ZOOKEEPER_CONNECTION_STRING" ]; then
        export DSTACK_MODULE_PROFILE_ZOOKEEPER=true
    fi
}

setup_gelf
setup_mysql
setup_redis
setup_zk

env | grep DSTACK | grep -v PASS | sort

exec java ${JAVA_OPTS:--Xmx256m} -jar $JAR "$@" $ARGS
