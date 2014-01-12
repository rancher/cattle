FROM ibuildthecloud/ubuntu-core-base:13.10
RUN yes | apt-get install --no-install-recommends openjdk-7-jre-headless
RUN yes | apt-get install --no-install-recommends openjdk-7-jdk
RUN yes | apt-get install --no-install-recommends maven
ADD . /usr/src/dstack
RUN /usr/src/dstack/tools/docker/buildenv.sh indocker
