#!/bin/bash

if [ "$LIBVIRT_TEST" = "true" ]; then
    echo 'TOX_DEPS+=libvirt-python' > ${0}-agent-env
    echo 'export LIBVIRT_TEST=true' >> ${0}-agent-env
fi
