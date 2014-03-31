MVN=install

build: build-env
	./tools/docker/build.sh mvn ${MVN}

run: build
	./tools/docker/build.sh run

build-env:
	cd ./tools/docker && \
	docker build -t cattle-buildenv .

enter: build-env
	./tools/docker/build.sh bash

clean: build-env
	./tools/docker/build.sh find -depth -name __pycache__ -type d -exec rm -rf {} \;
	./tools/docker/build.sh find -depth -name .tox -type d -exec rm -rf {} \;
	./tools/docker/build.sh mvn clean
	./tools/docker/build.sh rm -rf dist ./code/agent/src/agents/pyagent/dist ./tests/integration/.tox ./code/agent/src/agents/pyagent/.tox

test: build
	FORCE_DB=h2 ./tools/docker/build.sh ./tools/build/runtests.sh

bundle:
	./tools/docker/build.sh mvn -Drelease clean package

images: bundle
	./dist/package.sh

images-clean:
	./tools/build/checkin-test.sh images
