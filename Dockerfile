FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-7-jre-headless
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-7-jdk maven python-pip \
        git
RUN pip install --upgrade pip tox
RUN git clone --recursive https://github.com/ibuildthecloud/dstack.git /opt/dstack-git && \
    cd /opt/dstack-git && \
    git checkout master
ENV HOME /root
RUN cd /opt/dstack-git && \
    mvn install && \
    mvn dependency:go-offline && \
    mv /root/.m2 /opt/m2-base && \
    rm -rf /opt/m2-base/repository/io/github/ibuildthecloud
ADD . /opt/dstack
RUN /opt/dstack/dstack.sh build
EXPOSE 8080
CMD ["/opt/dstack/dstack.sh", "run"]
