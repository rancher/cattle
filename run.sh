#!/bin/bash
set -x

export MAVEN_OPTS="-XX:MaxPermSize=256m"
mvn -DskipTests=true -pl app -am  install
cd app
mvn -X -Dexec.mainClass=org.apache.cloudstack.launcher.Main exec:java "$@"
