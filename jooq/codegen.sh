#!/bin/bash

cd $(dirname $0)
pushd ..
mvn -am -pl jooq install
popd
mvn -Dexec.classpathScope=test -Dexec.mainClass=org.jooq.util.GenerationTool -Dexec.arguments="/codegen.xml" package exec:java
