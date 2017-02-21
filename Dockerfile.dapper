FROM azul/zulu-openjdk:8u72

ENV DAPPER_ENV API_VERSION ENVIRONMENTS
ENV DAPPER_RUN_ARGS --privileged
ENV DAPPER_SOURCE /usr/src/cattle
ENV DAPPER_OUTPUT dist
WORKDIR ${DAPPER_SOURCE}

# MySQL
ENV MYSQL_VERSION 5.5
ENV MYSQL_HOST 127.0.0.1
ENV MYSQL_TCP_PORT 13306

# Postgres
ENV PGSQL_VERSION 9.6
ENV PGHOST 127.0.0.1
ENV PGPASSWORD cattle
ENV PGUSER cattle
ENV PGDATABASE cattle

# Install Python and packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends python-pip iptables xz-utils git curl

# Hack to work around overlay issue
RUN pip uninstall -y py >/dev/null >/dev/null 2>&1 || true && \
    pip install --upgrade pip==6.0.3 tox==1.8.1 virtualenv==12.0.4

# Build Tools
ENV BUILD_TOOLS_VERSION 0.3.1
RUN mkdir /tmp/build-tools && \
    cd /tmp/build-tools && \
    curl -sSL -o build-tools.tar.gz https://github.com/rancherio/build-tools/archive/v${BUILD_TOOLS_VERSION}.tar.gz && \
    tar -xzvf build-tools.tar.gz && cp ./build-tools-${BUILD_TOOLS_VERSION}/bin/* /usr/local/bin && \
    rm -rf /tmp/build-tools

# Cache Maven stuff
RUN cp /usr/lib/jvm/zulu-8-amd64/jre/lib/security/US_export_policy.jar /usr/lib/jvm/zulu-8-amd64/jre/lib/security/local_policy.jar
RUN cd /tmp && \
    git clone https://github.com/ibuildthecloud/cattle.git && \
    cd cattle && \
    git checkout mvnw && \
    ./scripts/checkstyle && \
    ./mvnw package && \
    cd .. && \
    rm -rf cattle

# Install Docker
RUN curl -o /usr/bin/docker -sL -f https://get.docker.com/builds/Linux/x86_64/docker-1.10.3 && \
    chmod +x /usr/bin/docker

# Cache binaries
COPY ./resources/content/cattle-global.properties ${DAPPER_SOURCE}/resources/content/
RUN bash -x cattle-binary-pull

VOLUME /var/lib/docker

ENTRYPOINT ["./scripts/entry"]
CMD ["ci"]
