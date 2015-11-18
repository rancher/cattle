FROM rancher/dind:v1.9.0-rancher1
COPY ./scripts/bootstrap /scripts/bootstrap
RUN /scripts/bootstrap
WORKDIR /source
COPY ./scripts/build-cache /scripts/build-cache
RUN /scripts/build-cache
