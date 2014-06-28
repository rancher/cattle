#!/bin/bash

do_install()
{
    # Install docker if you don't have it
    [ ! -x "$(which docker)" ] && curl -sL https://get.docker.io/ | sh

    # Install libvirt too
    sudo apt-get install -y libvirt-bin python-libvirt qemu-kvm python-pip python-numpy arptables

    # Gonna need a ssh server
    sudo apt-get install -y openssh-server

    # CLI
    pip install cattle

    # Start Cattle
    sudo docker run -d -p 8080:8080 cattle/server

    # Download and authorize SSH key.
    while true; do
            curl -s http://localhost:8080/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
            if [ ${PIPESTATUS[0]} == 0 ]; then
                    break
            else
                    echo Waiting
                    sleep 5
            fi
    done

    # Register agent
    curl -X POST http://localhost:8080/v1/agents
}

do_install > /var/log/cattle-install.log 2>&1
