#!/bin/bash
set -e

cd $(dirname $0)

python_deps()
{
    if diff -q dist-requirements.txt dist/dist-requirements.txt >/dev/null 2>&1; then
        return 0
    fi

    VER=$(pip --version | awk '{print $2}')
    MAJOR=$(echo $VER | cut -f1 -d.)
    MINOR=$(echo $VER | cut -f2 -d.)
    if [ $MAJOR -lt 2 ] && [ $MINOR -lt 5 ]
    then
        echo "[ERROR] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 1>&2
        echo "[ERROR] !! pip 1.5 or newer is required !!" 1>&2
        echo "[ERROR] !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" 1>&2
        exit 1
    fi

    if [ -e dist ]; then
        rm -rf dist
    fi

    pip install -t dist -r dist-requirements.txt
    cp dist-requirements.txt dist
}

docker_build()
{
    cd binary-deps
    docker build -t agent-build .
    ID=$(docker run -d agent-build echo)
    docker cp ${ID}:/usr/bin/nsenter ../cattle/plugins/docker || true
    docker cp ${ID}:/usr/local/bin/socat ../cattle/plugins/core || true
}

local_build()
{
    if [ -e build ]; then
        rm -rf build
    fi

    mkdir -p build
    cd build

    ../binary-deps/build.sh

    cp util-linux*/nsenter ../cattle/plugins/docker/nsenter
    cp socat*/socat ../cattle/plugins/core/socat
}

binary_deps()
{
    if [[ -e ./cattle/plugins/core/socat && -e ./cattle/plugins/docker/nsenter ]]; then
        return 0
    fi

    if docker info >/dev/null 2>&1; then
        docker_build
    else
        local_build
    fi
}

python_deps
binary_deps
