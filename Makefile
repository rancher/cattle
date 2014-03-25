
build: build-env
	./tools/docker/build.sh

build-env:
	cd ./tools/docker && \
	docker build -t dstack-buildenv .

enter: build-env
	./tools/docker/build.sh enter

clean:
	MAVEN_TARGET='clean' ./tools/docker/build.sh

bundle: clean
	MAVEN_ARGS='-Drelease' ./tools/docker/build.sh

release-docker: bundle
	cd ./dist && \
	docker build -t dstack .
