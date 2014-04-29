#!/bin/bash
set -e pipefile

curl -s https://www.kernel.org/pub/linux/utils/util-linux/v2.24/util-linux-2.24.tar.xz | tar xvJf -
cd util-linux*
./configure
make -j4 nsenter
cd -

curl -s http://www.dest-unreach.org/socat/download/socat-1.7.2.4.tar.bz2 | tar xvjf -
cd socat*
LDFLAGS="-static" ./configure
make -j4 socat
