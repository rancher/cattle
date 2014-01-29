#!/bin/bash
set -e

cd $(dirname $0)/../..

mvn -B -DskipTests -Dnot-iaas -Dnot-implementation install
mvn -B -DskipTests -P!gdapi-java-server,!jedis,!jooq -Dnot-implementation install
mvn -B -P!gdapi-java-server,!jedis,!jooq install
