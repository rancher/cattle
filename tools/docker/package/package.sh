#!/bin/bash

BASE=${BASE:-cattle-}
TAG=${TAG:-latest}

BASE_IMAGE=${BASE}server:${TAG}

if [ "$TAG" != "latest" ]; then
    export BASE="cattle/"
    TAG= $0 "$@"
fi

cd $(dirname $0)

docker build -t $BASE_IMAGE server

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
    )
done
