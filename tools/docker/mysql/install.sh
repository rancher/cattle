#!/bin/bash

rm /etc/apt/apt.conf.d/no-cache

apt-get update
apt-get install -y mysql-server
apt-get install -y phpmyadmin
