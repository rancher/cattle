# The dstack-buildenv image is created by running tools/docker/buildenv.sh
FROM ibuildthecloud/dstack-buildenv
ADD . /usr/src/dstack
RUN /usr/src/dstack/dstack.sh build
CMD ["/usr/src/dstack/dstack.sh", "run"]
EXPOSE 8080
