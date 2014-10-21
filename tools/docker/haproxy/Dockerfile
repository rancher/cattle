FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && yes | apt-get -y install haproxy curl
ADD main.sh /
CMD ["/main.sh"]
EXPOSE 80
