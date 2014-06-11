FROM ibuildthecloud/ubuntu-core-base:14.04
RUN apt-get update && \
     apt-get install -y python-minimal curl python-eventlet
RUN apt-get update && apt-get install -y \
     busybox
RUN apt-get update && apt-get install -y \
     iptables
ADD nsenter /usr/sbin/nsenter
ADD startup.sh /
CMD ["/startup.sh"]
