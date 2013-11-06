#!/bin/bash
set -x

export MAVEN_OPTS="-XX:MaxPermSize=256m"
mvn -DskipTests=true install
cd app
mvn jetty:run "$@"
