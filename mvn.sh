#!/bin/bash

export MAVEN_OPTS="-XX:MaxPermSize=256m"
mvn -pl app -am "$@"
