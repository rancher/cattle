FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-7-jre-headless
ADD logstash-1.3.3-flatjar.jar /logstash.jar
ADD logstash.config /
CMD ["java", "-jar", "logstash.jar", "agent", "-f", "logstash.config"]
EXPOSE 12201/udp 9200
