#!/bin/bash
/etc/init.d/mysql start
/etc/init.d/apache2 start
PID=$(pidof /usr/sbin/mysqld)

while [ -e /proc/$PID ]; do
    sleep 1
done
