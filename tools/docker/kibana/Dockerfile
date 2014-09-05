FROM ibuildthecloud/ubuntu-core-base:14.04-1
RUN apt-get update && apt-get install -y apache2 curl
RUN curl https://download.elasticsearch.org/kibana/kibana/kibana-3.1.0.tar.gz | tar xvzf - -C /var/www/html --strip-components=1
ADD run.sh /
CMD ["/run.sh"]
EXPOSE 80
