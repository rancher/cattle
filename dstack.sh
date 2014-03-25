#!/bin/bash
set -e

if [ -z "$BUG_INCEPTION" ]; then
    # Work around issue https://github.com/dotcloud/docker/issues/4854
    export BUG_INCEPTION=true
    $0 "$@"
fi

BASE=$(dirname $0)
cd $BASE

trap fixperms EXIT

fixperms()
{
    if [ -n "$BUILD_USER_ID" ]; then
        cd $BASE
        echo "Assign files to $BUILD_USER_ID"
        chown -R $BUILD_USER_ID .
    fi
}

run()
{
    WAR="$(echo code/packaging/app/target/*war)"
    if [ ! -e "$WAR" ]; then
        mvn clean install
    fi

    WAR=$(readlink -f $WAR)
    mkdir -p runtime
    cd runtime

    exec java -jar $WAR "$@"
}

build()
{
    echo HOME=$HOME
    if [ ! -e $HOME/.m2 ] && [ -e /opt/m2-base ]; then
        cp -rf /opt/m2-base $HOME/.m2
    fi
    if [ "$#" = "0" ]; then
        mvn $MAVEN_ARGS ${MAVEN_TARGET:-install}
    else
        mvn "$@"
    fi
    mkdir -p dist/artifacts
    cp code/packaging/app/target/*.war dist/artifacts/dstack.jar
    if [ -e code/packaging/bundle/target/dstack-bundle*.jar ]; then
        if [ code/packaging/bundle/target/dstack-bundle*.jar -nt code/packaging/app/target/*.war ]; then
            cp code/packaging/bundle/target/dstack-bundle*.jar dist/artifacts/dstack.jar
        fi
    fi
    cp tools/docker/wrapper.sh dist/artifacts/dstack.sh
    cp tools/docker/Dockerfile.dist dist/Dockerfile
}

if [ "$#" = "0" ]; then
    echo "Usage:"
    echo -e "\t$0 (build|run)"
    echo -e "\t\t build: Compiles from source"
    echo -e "\t\t run: Starts dStack on port 8080 (will build if no build has been done)"
    exit 1
fi

case $1 in
mvn)
    shift
    build "$@"
    ;;
build)
    shift
    build "$@"
    ;;
run)
    shift
    run "$@"
    ;;
*)
    "$@"
    ;;
esac
