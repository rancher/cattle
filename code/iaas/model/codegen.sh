#!/bin/bash
set -e

cd $(dirname $0)
pushd ../../..
mvn -am -pl jooq,object install
popd
#mvn -Djooq.version=3.2.0 -Dexec.classpathScope=test -Dexec.mainClass=org.jooq.util.GenerationTool -Dexec.arguments="/codegen.xml" package exec:java
mvn -Dexec.classpathScope=test -Dexec.mainClass=org.jooq.util.GenerationTool -Dexec.arguments="/codegen.xml" package exec:java
