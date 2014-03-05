FROM ibuildthecloud/ubuntu-core-base:13.10
ADD answer.seed /root/
RUN debconf-set-selections < /root/answer.seed || true
RUN apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y mysql-server
RUN /etc/init.d/mysql start && \
    apt-get update && \
    DEBIAN_FRONTEND='noninteractive' apt-get install -y phpmyadmin
ADD create_db_and_user_dev.sql /root/
ADD my.cnf /etc/mysql/my.cnf
RUN /etc/init.d/mysql start && \
    mysql -u root < /root/create_db_and_user_dev.sql
ADD run.sh /
EXPOSE 80 3306
CMD [ "/run.sh" ]
