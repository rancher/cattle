#!/bin/bash

docker build -t cattle/libvirt:dev .
sudo bash << "EOF"
    docker run --rm \
        --privileged \
        -v /lib/modules:/lib/modules \
        -v /proc:/host/proc \
        -v /run:/host/run \
        -v /var:/host/var \
        cattle/libvirt:dev | bash
EOF
