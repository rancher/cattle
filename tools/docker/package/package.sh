#!/bin/bash
set -e

cd $(dirname $0)

JAR_VERSION=dev
if [ -e version ]; then
    JAR_VERSION=$(<version)
fi

BASE=${BASE:-cattle/}
export TAG=${TAG:-$JAR_VERSION}

BASE_IMAGE=${BASE}server:${TAG}
export IMAGE_REGISTRY=$(readlink -f images)

if [ -e ${IMAGE_REGISTRY} ]; then
    rm ${IMAGE_REGISTRY}
fi

cat > server/artifacts/image-version << EOF
CATTLE_IMAGE_NAME=${BASE}server
CATTLE_IMAGE_TAG=${TAG}
EOF

echo Building $BASE_IMAGE
docker build -t $BASE_IMAGE server
echo ${BASE_IMAGE} >> $IMAGE_REGISTRY
echo Done building $BASE_IMAGE

for i in api-server process-server agent-server; do
    (
        mkdir -p $i
        cd $i
        
        cat > Dockerfile << EOF
FROM $BASE_IMAGE
ENV CATTLE_SERVER_PROFILE $i
EOF

        echo Building ${BASE}${i}:${TAG}
        docker build -t ${BASE}${i}:${TAG} .
        echo ${BASE}${i}:${TAG} >> $IMAGE_REGISTRY
        echo Done building ${BASE}${i}:${TAG}
    )
done

for OTHER in $(find . -name build-image.sh); do
    echo Running $OTHER
    $OTHER
done

echo Done: TAG ${TAG}
