#!/bin/bash
set -e
set -x

cd $(dirname $0)/../..

VERSION=$1
TAG=v$1

if [ -z "$VERSION" ]; then
    echo "Usage: $0 TAG"
    exit 1
fi

git tag -d ${TAG} 2>/dev/null || true
rm release.properties 2>/dev/null || true

MAJOR=$(echo $VERSION | cut -f1 -d.)
MINOR=$(echo $VERSION | cut -f2 -d.)
PATCH=$(echo $VERSION | cut -f3 -d.)

NEXT_VERSION=${MAJOR}.$(($MINOR + 1)).0-SNAPSHOT

echo "Build Tag:    " $TAG
echo "Build Version:" $VERSION
echo "Next Version: " $NEXT_VERSION

if [ ! -e .gitconfig ]; then
    cp ~/.gitconfig .
fi

OPTS="-Drelease -Darguments=-Drelease -DlocalCheckout=true -DautoVersionSubmodules=true -DpushChanges=false -DreleaseVersion=$VERSION -Dtag=${TAG} -DdevelopmentVersion=$NEXT_VERSION"
make MVN="$OPTS org.apache.maven.plugins:maven-release-plugin:2.5:prepare" build
make MVN="$OPTS org.apache.maven.plugins:maven-release-plugin:2.5:perform" release-images
