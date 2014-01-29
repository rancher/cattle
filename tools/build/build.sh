#!/bin/bash
set -e

cd $(dirname $0)/../..

mvn -T1.5C -B -DskipTests -Dnot-iaas -Dnot-implementation install
mvn -T1.5C -B -DskipTests -P!gdapi-java-server,!jedis,!jooq -Dnot-implementation install
mvn -T1.5C -B -P!gdapi-java-server,!jedis,!jooq install
