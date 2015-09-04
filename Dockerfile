FROM rancher/dind:v0.6.0
COPY ./scripts/bootstrap /scripts/bootstrap
RUN /scripts/bootstrap
WORKDIR /source
COPY ./scripts/build-cache /scripts/build-cache
RUN /scripts/build-cache
