FROM rancher/build-cattle:v0.2.1
COPY ./scripts/bootstrap /scripts/bootstrap
RUN /scripts/bootstrap
WORKDIR /source
COPY ./scripts/build-cache /scripts/build-cache
RUN /scripts/build-cache
