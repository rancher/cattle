FROM ibuildthecloud/ubuntu-core-base:14.04
RUN apt-get update && apt-get install -y \
    busybox \
    curl \
    dnsmasq \
    iptables \
    monit \
    psmisc \
    uuid-runtime \
    vim-tiny && \
    rm -rf /var/lib/apt/lists
RUN apt-get update && apt-get install -y python-minimal
RUN curl -s https://bootstrap.pypa.io/get-pip.py > get-pip.py; python get-pip.py; rm get-pip.py
RUN pip install cattle docker-py
RUN curl -s http://stedolan.github.io/jq/download/linux64/jq > /usr/bin/jq; chmod +x /usr/bin/jq
RUN curl -s https://get.docker.io/builds/Linux/x86_64/docker-latest > /usr/bin/docker; chmod +x /usr/bin/docker
ADD register.sh /
ADD register.py /
CMD ["/register.sh"]
