#!/bin/bash
set -e

cd $(dirname $0)/../..

mvn -DskipTests -Dnot-iaas -Dnot-implementation install
mvn -DskipTests -P!gdapi-java-server,!jedis,!jooq -Dnot-implementation install
mvn -P!gdapi-java-server,!jedis,!jooq install
