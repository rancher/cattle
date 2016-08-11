FROM rancher/build-cattle:1.10.3-rancher1
COPY ./scripts/bootstrap /scripts/bootstrap
RUN /scripts/bootstrap
WORKDIR /source
COPY ./scripts/build-cache /scripts/build-cache

# Adding static binaries
ADD https://github.com/wlan0/machine/releases/download/v0.3.0-dev-packet/docker-machine /usr/bin/docker-machine
RUN chmod +x /usr/bin/docker-machine

ADD https://github.com/rancherio/go-machine-service/releases/download/v0.12.0/go-machine-service.tar.xz /tmp/
RUN tar xpvf /tmp/go-machine-service.tar.xz -C /usr/bin/ && \
    chmod +x /usr/bin/go-machine-service


RUN /scripts/build-cache
