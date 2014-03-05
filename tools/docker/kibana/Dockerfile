FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && apt-get install -y apache2 curl
RUN curl https://download.elasticsearch.org/kibana/kibana/kibana-3.0.0milestone5.tar.gz | tar xvzf - -C /var/www/ --strip-components=1
ADD run.sh /
CMD ["/run.sh"]
EXPOSE 80
