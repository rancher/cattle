FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && \
     apt-get install -y redis-server
CMD ["/usr/bin/redis-server"]
EXPOSE 6379
