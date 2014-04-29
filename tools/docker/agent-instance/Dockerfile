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
ADD startup.sh /startup.sh
CMD ["/startup.sh"]
