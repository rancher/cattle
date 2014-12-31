FROM rancher/docker-dind-base:latest
COPY ./scripts/bootstrap /scripts/bootstrap
RUN /scripts/bootstrap
WORKDIR /source
RUN mkdir -p /usr/src/cattle; cd /usr/src/cattle; git clone https://github.com/cattleio/cattle.git .
RUN cd /usr/src/cattle; mvn -DskipTests=true install || true
RUN cd /usr/src/cattle; mvn dependency:go-offline || true
