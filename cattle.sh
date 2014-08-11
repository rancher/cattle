#!/bin/bash
set -e

if [ -z "$BUG_INCEPTION" ]; then
    # Work around issue https://github.com/dotcloud/docker/issues/4854
    export BUG_INCEPTION=true
    $0 "$@"
    exit $?
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
    if [ ! -e $HOME/.m2/repository/io/github/cattle ] && [ -e deps/cattle ]; then
        (
            cd deps/cattle
            mvn -DskipTests=true install
        )
    fi
    if [ "$#" = "0" ]; then
        mvn $MAVEN_ARGS ${MAVEN_TARGET:-install}
    else
        mvn "$@"
    fi
    mkdir -p dist/server/artifacts

    local max=0
    local files=(code/packaging/app/target/*.war
             code/packaging/bundle/target/cattle-bundle*.jar
             target/checkout/.m2/io/cattle/cattle-bundle/*/cattle-bundle-[0-9].[0-9].[0-9].jar)

    local src=
    local target=dist/server/artifacts/cattle.jar

    for i in ${files[@]}; do
        if [ -e "$i" ]; then
            if [ "$src" = "" ] || [ $i -nt $src ]; then
                src=$i
            fi
        fi
    done

    if [ -e "$src" ] && [ ! -e $target ] || [ "$src" -nt "$target" ]; then
        echo "Copying $src => $target"
        cp $src $target
        if [[ $src =~ .*jar$ ]]; then
            java -jar $target version > dist/version
        fi
    fi

    tar c -C tools/docker/package . | tar xf - -C dist
}

if [ "$#" = "0" ]; then
    echo "Usage:"
    echo -e "\t$0 (build|run)"
    echo -e "\t\t build: Compiles from source"
    echo -e "\t\t run: Starts Cattle on port 8080 (will build if no build has been done)"
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
