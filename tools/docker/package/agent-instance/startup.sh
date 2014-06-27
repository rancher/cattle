#!/bin/bash
set -e

cleanup()
{
    local exit=$?

    if [ -e "$TEMP_DOWNLOAD" ]; then
        rm -rf $TEMP_DOWNLOAD
    fi

    if [ "$exit" != "0" ]; then
        kill 1
    fi

    return $exit
}

trap cleanup EXIT

# This is copied from common/scripts.sh, if there is a change here
# make it in common and then copy here
check_debug()
{
    if [ -n "$CATTLE_SCRIPT_DEBUG" ] || echo "${@}" | grep -q -- --debug; then
        export CATTLE_SCRIPT_DEBUG=true
        export PS4='[${BASH_SOURCE##*/}:${LINENO}] '
        set -x
    fi
}

info()
{
    echo "INFO:" "${@}"
}

error()
{
    echo "ERROR:" "${@}" 1>&2
}

export CATTLE_HOME=${CATTLE_HOME:-/var/lib/cattle}

check_debug
# End copy

CONTENT_URL=/configcontent/configscripts
INSTALL_ITEMS="configscripts agent-instance-startup"


call_curl()
{
    local curl="curl -s" 
    if [ -n "$CATTLE_AGENT_INSTANCE_AUTH" ]; then
        $curl -H "Authorization: $CATTLE_AGENT_INSTANCE_AUTH" "$@"
    elif [ -n "$CATTLE_ACCESS_KEY" ]; then
        $curl -u ${CATTLE_ACCESS_KEY}:${CATTLE_SECRET_KEY} "$@"
    else
        $curl "$@"
    fi
}

download_agent()
{
    TEMP_DOWNLOAD=$(mktemp -d bootstrap.XXXXXXX)
    info Downloading agent "${CATTLE_CONFIG_URL}${CONTENT_URL}"
    call_curl --retry 5 ${CATTLE_CONFIG_URL}${CONTENT_URL} > $TEMP_DOWNLOAD/content
    tar xzf $TEMP_DOWNLOAD/content -C $TEMP_DOWNLOAD
    bash $TEMP_DOWNLOAD/*/config.sh --force $INSTALL_ITEMS
}

setup_config_url()
{
    if [ -n "$CATTLE_CONFIG_URL" ]; then
        return
    fi

    local host=$(ip route show dev eth0 | grep ^default | awk '{print $3}')
    CATTLE_CONFIG_URL="${CATTLE_CONFIG_URL_SCHEME:-http}"
    CATTLE_CONFIG_URL="${CATTLE_CONFIG_URL}://${CATTLE_CONFIG_URL_HOST:-$host}"
    CATTLE_CONFIG_URL="${CATTLE_CONFIG_URL}:${CATTLE_CONFIG_URL_PORT:-9342}"
    CATTLE_CONFIG_URL="${CATTLE_CONFIG_URL}${CATTLE_CONFIG_URL_PATH:-/v1}"

    export CATTLE_CONFIG_URL
}

start()
{
    mkdir -p $CATTLE_HOME
    cd $CATTLE_HOME

    # Let scripts know its being ran during startup
    export CATTLE_AGENT_STARTUP=true

    setup_config_url
    download_agent
}

if [ "$1" = "start" ]; then
    start
elif [ "$1" = "init" ]; then
    export -p > /.dockerenv-save
    touch /etc/agent-instance
    echo '::sysinit:bash /etc/init.d/agent-instance-startup start' > /etc/inittab
    if [ ! -e /init ]; then
        ln -s /bin/busybox /init
    fi
    cd /
    exec ./init
fi
