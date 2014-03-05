#!/bin/bash
set -e

cd $(dirname $0)

rm /etc/apt/apt.conf.d/no-cache 2>/dev/null || true
apt-get update
apt-get install -y build-essential ncurses-dev wget

wget https://www.kernel.org/pub/linux/utils/util-linux/v2.24/util-linux-2.24.1.tar.xz
tar xvJf util-linux-2.24.1.tar.xz
cd util-linux-2.24.1
./configure
make -j4
cp nsenter ..
