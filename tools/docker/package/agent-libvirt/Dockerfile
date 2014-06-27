FROM ibuildthecloud/ubuntu-core-base:14.04-1
RUN apt-get update && \
     apt-get install -y python-minimal curl python-eventlet \
     iptables \
     qemu-kvm \
     libvirt-bin \
     rsync
# This is to work around some AppArmor issues on ubuntu
RUN cp /usr/sbin/libvirtd /usr/sbin/cattle-libvirtd
ADD /startup.sh /startup.sh
CMD ["/startup.sh"]
