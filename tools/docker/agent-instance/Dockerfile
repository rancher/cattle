FROM ibuildthecloud/ubuntu-core-base:14.04
ADD http://stedolan.github.io/jq/download/linux64/jq /usr/bin/
RUN chmod +x /usr/bin/jq
RUN apt-get update && apt-get install -y \
    busybox \
    curl \
    dnsmasq \
    iptables \
    monit \
    nodejs \
    psmisc \
    uuid-runtime \
    vim-tiny && \
    rm -rf /var/lib/apt/lists
RUN ln -s /usr/bin/nodejs /usr/bin/node
ADD startup.sh /etc/init.d/agent-instance-startup
CMD ["/etc/init.d/agent-instance-startup", "init"]
RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y racoon
