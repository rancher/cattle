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
UPTODATE=false

URL=$CATTLE_CONFIG_URL
AUTH=${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY}
URL_SUFFIX=/configcontent/

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

if [ "${CATTLE_CONFIG_FLOCKER}" != "$LOCK" ]; then
    if ! CATTLE_CONFIG_FLOCKER="$LOCK" flock -oe -n "$LOCK" true; then
        echo -n Lock failed
        exit 122
    fi
    CATTLE_CONFIG_FLOCKER="$LOCK" flock -oe -n "$LOCK" "$0" "$@"
    exit $?
fi


download()
{
    cleanup

    DOWNLOAD_TEMP=$(mktemp -d ${DOWNLOAD}.XXXXXXX)

    if [ -z "$URL" ] || [ -z "$AUTH" ]
    then
        error "Both --url and --auth must be supplied"
        exit 1
    fi

    local name=$1
    local current
    local archive_version

    if [ "$FORCE" != "true" ] && [ -e "${DOWNLOAD}/$name/current" ]; then
        current=$(<${DOWNLOAD}/$name/current)
    fi

    DOWNLOAD_URL=${URL}/${URL_SUFFIX}/$name

    local get_opts
    local archive_version
    if [ -n "$ARCHIVE_URL" ]; then
        DOWNLOAD_URL=${ARCHIVE_URL}
        get_opts="--no-auth"
        archive_version=$(echo "$ARCHIVE_URL" | md5sum | awk '{print $1}')
        if [ "$archive_version" = "$current" ]; then
            info $DOWNLOAD_URL already downloaded
            UPTODATE=true
            VERSION=$current
            return 0
        fi
    fi

    info Downloading $DOWNLOAD_URL "current=$current"

    get $get_opts "$DOWNLOAD_URL?current=$current" > $DOWNLOAD_TEMP/download
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

    if [ -n "$archive_version" ]; then
        mv ${DOWNLOAD_TEMP}/$dir ${DOWNLOAD_TEMP}/${archive_version}

        dir=$archive_version
        echo "$archive_version" > ${DOWNLOAD_TEMP}/${dir}/version
    fi

    content_root=${DOWNLOAD}/$name/${dir}

    if [ -e ${DOWNLOAD_TEMP}/${dir}/uptodate ] && [ -e ${content_root}/version ]; then
        UPTODATE=true
        VERSION=$(<${content_root}/version)
        info "Already up to date"
        return 0
    fi


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

        info "Running ${content_root}/check.sh"
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

    VERSION=$(<${content_root}/version)
    info "Running ${content_root}/apply.sh"
    pushd ${content_root} >/dev/null
    ./apply.sh "${opts[@]}" || CATTLE_SCRIPT_DEBUG=true ./apply.sh "${opts[@]}"
    echo $(basename $(pwd)) > ../current
    popd >/dev/null
}

applied()
{
    info Sending $1 applied ${dir} ${VERSION}
    put "${DOWNLOAD_URL}?version=${VERSION}" > /dev/null
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
    --force)
        FORCE=true
        ;;
    --env)
        docker_env_vars
        exit 0
        ;;
    --archive-url)
        shift 1
        ARCHIVE_URL=$1
        ;;
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
        info "Updating" "$1"
        UPTODATE=false
        download $1
        if [ "$UPTODATE" != "true" ]; then
            apply
        fi
        if [ -z "$ARCHIVE_URL" ]; then
            applied $1
        fi
        ;;
    esac
    shift 1
done
