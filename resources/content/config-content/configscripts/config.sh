#!/bin/bash
set -e

trap cleanup EXIT

source $(dirname $0)/common/scripts.sh

LOCK=$CATTLE_HOME/config.lock
DOWNLOAD=$CATTLE_HOME/download

URL=$CATTLE_CONFIG_URL
AUTH=${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}
URL_SUFFIX=/configcontent/
DOWNLOAD_TEMP=$(mktemp -d ${DOWNLOAD}.XXXXXXX) 

cleanup()
{
    if [ -e "$DOWNLOAD_TEMP" ]; then
        rm -rf $DOWNLOAD_TEMP
    fi
}

lock()
{
    exec 200>$LOCK
    flock -n -x 200
}

download()
{
    if [ -z "$URL" ] || [ -z "$AUTH" ]
    then
        error "Both --url and --auth must be supplied"
        exit 1
    fi

    DOWNLOAD_URL=${URL}/${URL_SUFFIX}/$1

    echo Downloading $DOWNLOAD_URL
    curl -s -u $AUTH $DOWNLOAD_URL > $DOWNLOAD_TEMP/download
    tar xzf $DOWNLOAD_TEMP/download -C $DOWNLOAD_TEMP
    rm $DOWNLOAD_TEMP/download

    local dir=$(basename $(ls -1 $DOWNLOAD_TEMP))
    if [ ! -e "$DOWNLOAD_TEMP/$dir" ]; then
        error "Invalid archive format got $dir for root folder"
        dump $DOWNLOAD_TEMP
        exit 1
    fi

    content_root=${DOWNLOAD}/${dir}

    if [ -e ${content_root} ]; then
        rm -rf ${content_root}
    fi

    mv ${DOWNLOAD_TEMP}/$dir ${content_root}
}

apply()
{
    if [ ! -e ${content_root}/apply.sh ]; then
        error Missing ${content_root}/apply.sh
        dump ${content_root}
        exit 1
    fi

    if [ ! -x ${content_root}/apply.sh ]; then
        chmod +x ${content_root}/apply.sh
    fi

    version=$(<${content_root}/version)
    echo "Running ${content_root}/apply.sh [$version]"
    ${content_root}/apply.sh "${opts[@]}"
}

applied()
{
    echo Sending applied ${dir} ${version}
    curl -X PUT -s -u $AUTH "${DOWNLOAD_URL}?version=${version}" > /dev/null
}

dump()
{
    echo Content
    find $1
}

mkdir -p $CATTLE_HOME
mkdir -p $DOWNLOAD

lock

opts=()
while [ "$#" -gt 0 ]; do
    case $1 in
    --url)
        shift 1
        URL=$1
        ;;
    --auth)
        AUTH=$1
        ;;
    --*)
        opts+=($1)
        ;;
    *)
        download $1
        apply
        applied
        ;;
    esac
    shift 1
done
