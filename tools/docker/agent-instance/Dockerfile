FROM cattle/agent:latest
ADD http://stedolan.github.io/jq/download/linux64/jq /usr/bin/
RUN chmod +x /usr/bin/jq
RUN apt-get update && apt-get install -y \
    curl \
    vim \
    monit \
    dnsmasq \
    busybox \
    psmisc \
    uuid-runtime
RUN ln -s /bin/busybox /init
ADD inittab /etc/inittab
ADD monitrc /etc/monitrc
RUN chmod 600 /etc/monitrc
ADD startup.sh /etc/init.d/agent-instance-startup
RUN touch /etc/agent-instance
CMD ["/init"]
