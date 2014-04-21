FROM ibuildthecloud/ubuntu-core-base:14.04
RUN rm /etc/apt/apt.conf.d/no-cache
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-7-jre-headless
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-7-jdk maven python-pip \
        git net-tools curl
RUN sed -i 's/^#deb-src/deb-src/g' /etc/apt/sources.list
RUN apt-get update && apt-get build-dep -y util-linux socat
RUN pip install --upgrade pip tox
RUN git clone --recursive https://github.com/cattleio/cattle.git /opt/cattle-git && \
    cd /opt/cattle-git && \
    git checkout master
ENV HOME /root
RUN cd /opt/cattle-git && \
    mvn install && \
    mvn dependency:go-offline && \
    mv /root/.m2 /opt/m2-base && \
    rm -rf /opt/m2-base/repository/io/github/cattle
