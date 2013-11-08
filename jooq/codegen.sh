#!/bin/bash

mvn -Dexec.classpathScope=test -Dexec.mainClass=org.jooq.util.GenerationTool -Dexec.arguments="/codegen.xml" package exec:java
