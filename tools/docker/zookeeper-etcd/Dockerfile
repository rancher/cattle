FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && \
     apt-get install -y --no-install-recommends openjdk-7-jre-headless python-pip
RUN pip install requests etcd-py
ADD http://apache.cs.utah.edu/zookeeper/zookeeper-3.4.5/zookeeper-3.4.5.tar.gz /
RUN mv /zookeeper-3.4.5.tar.gz/zookeeper-3.4.5 /zookeeper && rm -rf /zookeeper-3.4.5.tar.gz
ADD startup.py /zookeeper/bin/startup.py
RUN chmod +x /zookeeper/bin/startup.py
CMD ["/zookeeper/bin/startup.py"]
EXPOSE 2181 2888 3888
