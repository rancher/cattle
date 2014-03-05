FROM ubuntu:12.04
RUN apt-get update #1393639690
RUN apt-get install -y --no-install-recommends openjdk-7-jre-headless
RUN apt-get install -y --no-install-recommends openjdk-7-jdk
RUN apt-get install -y --no-install-recommends maven python-pip git
RUN pip install --upgrade pip tox
# Cache maven deps
RUN git clone https://github.com/ibuildthecloud/dstack.git /opt/dstack-git && \
    git check prev
RUN cd /opt/dstack-git && \
    mvn dependency:go-offline
ADD . /opt/dstack
RUN /opt/dstack/dstack.sh build
EXPOSE 8080
CMD ["/opt/dstack/dstack.sh", "run"]
