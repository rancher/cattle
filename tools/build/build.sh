#!/bin/bash
set -e

cd $(dirname $0)/../..

mvn -B ${MAVEN_ARGS} -DskipTests -Dnot-iaas -Dnot-implementation install
mvn -B ${MAVEN_ARGS} -DskipTests -P!gdapi-java-server,!jedis,!jooq -Dnot-implementation install
mvn -B ${MAVEN_ARGS} -P!gdapi-java-server,!jedis,!jooq install
