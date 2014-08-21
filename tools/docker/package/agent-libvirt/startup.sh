#!/bin/bash
set -e

trap "exit 1" SIGINT SIGTERM

PID_FILE=/run/libvirtd.pid

mkdir -p /run/libvirt
mkdir -p /var/lib/cattle
mkdir -p /host/run/cattle/libvirt
mkdir -p /host/var/lib/cattle/libvirt

if [ ! -e /host/var/lib/cattle/etc/libvirt ]; then
    rsync -a /etc/libvirt/ /host/var/lib/cattle/etc/libvirt >/dev/null 2>&1
fi

mount --bind /host/proc /proc
mount --bind /host/sys /sys
mount --bind /host/dev /dev
mount --bind /host/run/cattle/libvirt /run
mount --bind /host/var/lib/cattle/etc/libvirt /etc/libvirt
mount --bind /host/var/lib/cattle/libvirt /var/lib/libvirt
mount --bind /host/var/lib/cattle /var/lib/cattle

if [ -e $PID_FILE ]; then
    rm $PID_FILE
fi

if [ ! -e /dev/kvm ]; then
    mknod /dev/kvm c 10 232
fi

chown root:kvm /dev/kvm
chmod ug+rw /dev/kvm

cat << EOF
PID=\$(docker inspect --format '{{.State.Pid}}' $(hostname))
nsenter -m -t \$PID -- env -i /usr/sbin/cattle-libvirtd -d
EOF

sleep 10

while true; do
    sleep 1
    if [ ! -e $PID_FILE ] || [ ! -e /proc/$(<$PID_FILE) ]; then
        break
    fi
done
