
build: build-env
	./tools/docker/build.sh mvn install

build-env:
	cd ./tools/docker && \
	docker build -t dstack-buildenv .

enter: build-env
	./tools/docker/build.sh bash

clean: build-env
	./tools/docker/build.sh find -depth -name __pycache__ -type d -exec rm -rf {} \;
	./tools/docker/build.sh find -depth -name .tox -type d -exec rm -rf {} \;
	./tools/docker/build.sh mvn clean
	./tools/docker/build.sh rm -rf runtime

test: build
	FORCE_DB=h2 ./tools/docker/build.sh ./tools/build/runtests.sh

bundle: clean
	./tools/docker/build.sh mvn -Drelease clean package

release-docker-clean:
	./tools/build/checkin-test.sh release

release-docker: bundle
	cd ./dist && \
	docker build -t dstack .
