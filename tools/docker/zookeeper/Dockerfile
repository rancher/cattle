FROM ibuildthecloud/ubuntu-core-base:14.04-1
RUN apt-get update && \
     apt-get install -y --no-install-recommends openjdk-7-jre-headless python-requests
ADD http://mirrors.ibiblio.org/apache/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz /
#COPY zookeeper-3.4.6.tar.gz /
RUN tar xvzf zookeeper-3.4.6.tar.gz && \
    mv /zookeeper-3.4.6 /zookeeper && \
    rm -rf zookeeper-3.4.6.tar.gz
ADD startup.py /zookeeper/bin/startup.py
RUN chmod +x /zookeeper/bin/startup.py
CMD ["/zookeeper/bin/startup.py"]
EXPOSE 2181 2888 3888
