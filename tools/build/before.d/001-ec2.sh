#!/bin/bash
set -e -x

cd $(dirname $0)

if [ "$EC2_TEST" != "true" ]; then
    exit 0
fi

if [ ! -x "$(which aws)" ]; then
    sudo pip install awscli
fi

if [ ! -e key.pub ]; then
    ssh-keygen -t rsa -f key -N ''
fi

cat > user-data << EOF
#!/bin/bash

terminate()
{
    sleep 3000
    poweroff -f
}

setup()
{
    mkdir -p /root/.ssh
    mkdir -p /mnt/cattle
    mkdir -p /mnt/cattle-src
    ln -s /mnt/cattle /var/lib
    ln -s /mnt/cattle-src /usr/src/cattle
    cd /usr/src/cattle
    add-apt-repository -y ppa:fkrull/deadsnakes

    apt-get update
    apt-get install git
    git clone https://github.com/cattleio/cattle.git .

    echo '$(<key.pub)' > /root/.ssh/authorized_keys
    curl -sL https://get.docker.io/ | sh
    apt-get install -y python-eventlet python-pip libvirt-dev libvirt-bin python-dev python2.6-dev qemu-kvm python-libvirt arptables genisoimage
    if [ ! -e /usr/lib/libvirt-lxc.so ]; then
        ln -s /usr/lib/libvirt-lxc.so.0 /usr/lib/libvirt-lxc.so
    fi
    pip install --upgrade pip tox

    touch /var/tmp/setup-done
}

terminate &
setup 2>&1 | tee /var/log/setup.log
EOF

# 12.04 doesn't work, libvirt is too old for testing
#AMI=${AMI:-ami-a498a4e1}
AMI=${AMI:-ami-d8ac909d}
for ((i=0;i<10;i++)); do
    ID=$(aws ec2 run-instances --image-id $AMI                            \
                        --security-group-ids all                          \
                        --user-data file://user-data                      \
                        --instance-initiated-shutdown-behavior terminate  \
                        --query 'Instances[0].InstanceId'                 \
                        --output text)
    if [ -n "$ID" ]; then
        break
    fi
done

if [ -z "$ID" ]; then
    echo 'Failed to create EC2 node'
    exit 1
fi

echo $ID > ec2-id
