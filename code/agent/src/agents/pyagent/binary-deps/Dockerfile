FROM ibuildthecloud/ubuntu-core-base:14.04
RUN sed -i 's/^#deb-src/deb-src/g' /etc/apt/sources.list
RUN apt-get update && apt-get build-dep -y util-linux socat
RUN apt-get update && apt-get install -y curl
ADD build.sh /build.sh
RUN /build.sh
ADD install.sh /install.sh
RUN /install.sh
