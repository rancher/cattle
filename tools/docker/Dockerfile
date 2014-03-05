FROM ibuildthecloud/ubuntu-core-base:13.10
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-7-jre-headless
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-7-jdk maven python-pip
RUN pip install --upgrade pip tox
