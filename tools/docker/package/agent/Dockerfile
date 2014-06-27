FROM ibuildthecloud/ubuntu-core-base:14.04-1
RUN apt-get update && \
     apt-get install --no-install-recommends -y \
     arptables \
     ca-certificates \
     curl \
     iptables \
     python-eventlet \
     python-libvirt \
     python-minimal \
     python-numpy \
     qemu-utils
RUN curl -s https://bootstrap.pypa.io/get-pip.py > get-pip.py && python get-pip.py && rm get-pip.py
RUN pip install cattle docker-py websockify
RUN curl -s http://stedolan.github.io/jq/download/linux64/jq > /usr/bin/jq; chmod +x /usr/bin/jq
RUN curl -s https://get.docker.io/builds/Linux/x86_64/docker-latest > /usr/bin/docker; chmod +x /usr/bin/docker
ADD agent-env.sh /
ADD register.py /
CMD ["/agent-env.sh"]
