#!/bin/bash

if [ "$LIBVIRT_TEST" = "true" ]; then
    echo 'export LIBVIRT_TEST=true' >> ${0}-agent-env
fi
