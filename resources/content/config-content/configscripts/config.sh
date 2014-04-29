#!/bin/bash

set -e

source $(dirname $0)/common/scripts.sh

cleanup()
{
    EXIT=$?

    if [ -e "$DOWNLOAD_TEMP" ]; then
        rm -rf $DOWNLOAD_TEMP
    fi

    return $EXIT
}

trap cleanup EXIT

LOCK_DIR=${CATTLE_HOME}/locks
LOCK=${LOCK_DIR}/config.lock
DOWNLOAD=$CATTLE_HOME/download

URL=$CATTLE_CONFIG_URL
AUTH=${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}
URL_SUFFIX=/configcontent/
DOWNLOAD_TEMP=$(mktemp -d ${DOWNLOAD}.XXXXXXX) 

if [ ! -x $0 ]; then
    chmod +x $0
fi

if [ ! -d ${LOCK_DIR} ]; then
    mkdir -p ${LOCK_DIR}
fi

if [ "$CATTLE_AGENT_STARTUP" != "true" ] && [ -e /etc/agent-instance ]; then
    for i in {1..3}; do
        if [ ! -e /dev/shm/agent-instance-started ]; then
            sleep 2
        else
            break
        fi
    done

    if [ ! -e /dev/shm/agent-instance-started ]; then
        error "Agent instance has not started"
        exit 1
    fi
fi

[ "${FLOCKER}" != "$LOCK" ] && exec env FLOCKER="$LOCK" flock -oe -w 5 "$LOCK" "$0" "$@" || :

download()
{
    if [ -z "$URL" ] || [ -z "$AUTH" ]
    then
        error "Both --url and --auth must be supplied"
        exit 1
    fi

    local name=$1
    DOWNLOAD_URL=${URL}/${URL_SUFFIX}/$name

    info Downloading $DOWNLOAD_URL
    get $DOWNLOAD_URL > $DOWNLOAD_TEMP/download
    tar xzf $DOWNLOAD_TEMP/download -C $DOWNLOAD_TEMP
    rm $DOWNLOAD_TEMP/download

    local dir=$(basename $(ls -1 $DOWNLOAD_TEMP))
    if [ ! -e "$DOWNLOAD_TEMP/$dir" ]; then
        error "Invalid archive format got $dir for root folder"
        dump $DOWNLOAD_TEMP
        exit 1
    fi

    (
        cd $DOWNLOAD_TEMP

        if [ ! -e $dir/SHA1SUMSSUM ] || [ ! -e $dir/SHA1SUMS ]; then
            error "Missing SHA1SUMS files, invalid download"
            exit 1
        fi

        sha1sum -c $dir/SHA1SUMSSUM
        sha1sum -c $dir/SHA1SUMS
    ) >/dev/null

    content_root=${DOWNLOAD}/$name/${dir}

    if [ -e ${content_root} ]; then
        rm -rf ${content_root}
    fi

    mkdir -p $(dirname $content_root)
    mv ${DOWNLOAD_TEMP}/$dir ${content_root}
}

check_applied()
{
    local current=${content_root}/../current
    if [ -e $current ] && [ "$(<$current)" = "$(basename $content_root)" ]; then
        return 0
    fi

    return 1
}

apply()
{
    if check_applied && [ -e ${content_root}/check.sh ]; then
        if [ ! -x ${content_root}/check.sh ]; then
            chmod +x ${content_root}/check.sh
        fi

        info "Running ${content_root}/check.sh [$version]"
        cd ${content_root}
        if ./check.sh; then
            return
        fi
    fi

    if [ ! -e ${content_root}/apply.sh ]; then
        info Using default apply.sh
        cp ${CATTLE_HOME}/common/apply.sh ${content_root}
    fi

    if [ ! -x ${content_root}/apply.sh ]; then
        chmod +x ${content_root}/apply.sh
    fi

    version=$(<${content_root}/version)
    info "Running ${content_root}/apply.sh [$version]"
    cd ${content_root}
    ./apply.sh "${opts[@]}"
    echo $(basename $(pwd)) > ../current
}

applied()
{
    info Sending $1 applied ${dir} ${version}
    put "${DOWNLOAD_URL}?version=${version}" > /dev/null
}

dump()
{
    info Content
    find $1
}

mkdir -p $CATTLE_HOME
mkdir -p $DOWNLOAD

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
        applied $1
        ;;
    esac
    shift 1
done
