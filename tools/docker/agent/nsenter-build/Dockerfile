FROM ibuildthecloud/ubuntu-core-base:13.10
RUN rm /etc/apt/apt.conf.d/no-cache 2>/dev/null || true
RUN apt-get update
RUN apt-get install -y build-essential ncurses-dev wget ca-certificates
ADD nsenter-build.sh /
RUN /nsenter-build.sh
