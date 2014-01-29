#!/bin/bash

cd /var/lib/dstack
exec java $JAVA_OPTS -jar dstack.war
