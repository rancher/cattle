
build:
	./tools/docker/build.sh

clean:
	MAVEN_TARGET='clean' ./tools/docker/build.sh

release: clean
	MAVEN_ARGS='-Drelease' ./tools/docker/build.sh

release-docker: release
	cd ./dist && \
	docker build -t dstack .
