#!/bin/bash
set -e

trap cleanup EXIT

# This is copied from common/scripts.sh, if there is a change here
# make it in common and then copy here
check_debug()
{
    export DSTACK_HOME=${DSTACK_HOME:-/var/lib/dstack}

    if [ -n "$DSTACK_SCRIPT_DEBUG" ] || echo "${@}" | grep -q -- --debug; then
        export DSTACK_SCRIPT_DEBUG=true
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

check_debug
# End copy

CONF=/etc/dstack/agent/bootstrap.conf
CONTENT_URL=/configcontent/configscripts
INSTALL_ITEMS="configscripts pyagent"

cleanup()
{
    if [ -e "$TEMP_DOWNLOAD" ]; then
        rm -rf $TEMP_DOWNLOAD
    fi

    if [ -e $0 ] && echo $0 | grep -q ^/tmp
    then
        rm $0
    fi
}

download_agent()
{
    cleanup

    TEMP_DOWNLOAD=$(mktemp -d bootstrap.XXXXXXX)
    info Downloading agent "${DSTACK_URL}${CONTENT_URL}"
    curl -s -u $DSTACK_ACCESS_KEY:$DSTACK_SECRET_KEY ${DSTACK_URL}${CONTENT_URL} > $TEMP_DOWNLOAD/content
    tar xzf $TEMP_DOWNLOAD/content -C $TEMP_DOWNLOAD || ( cat $TEMP_DOWNLOAD/content 1>&2 && exit 1 )
    bash $TEMP_DOWNLOAD/*/config.sh --no-start $INSTALL_ITEMS
}

start_agent()
{
    local main=${DSTACK_HOME}/pyagent/apply.sh
    export AGENT_PARENT_PID=$$
    info Starting agent $main
    $main --no-daemon start &
    AGENT_PID=$!
}

get_line()
{  
    read -t 5 LINE || true
    if [ -z "$LINE" ]; then
        sleep 1
    fi
}

print_config()
{
    info Access Key: $DSTACK_ACCESS_KEY
    info Config URL: $DSTACK_CONFIG_URL
    info Storage URL: $DSTACK_STORAGE_URL
    info API URL: $DSTACK_URL
}

cd $(dirname $0)

if [ -e $CONF ]
then
    source $CONF
fi

OLD_LINE=
AGENT_PID=
while get_line; do
    if [ -n "$AGENT_PID" ] && [ ! -e /proc/$AGENT_PID ]; then
        error "Agent pid=$AGENT_PID has died"
        exit 1
    fi

    if [ -z "$LINE" ]; then
        continue
    fi

    if [ -n "$AGENT_PID" ] && [ "$LINE" != "$OLD_LINE" ]; then
        info Environment has changed, exiting
        info "Killing agent, pid=$AGENT_PID"
        kill $AGENT_PID
        info "Waiting on agent to die, pid=$AGENT_PID"
        wait $AGENT_PID
        info Exiting
        exit 0
    fi

    if [ -n "$AGENT_PID" ]; then
        continue
    fi
        
    OLD_LINE=$LINE

    echo $LINE
    eval "$LINE"
    check_debug
    print_config

    mkdir -p $DSTACK_HOME
    download_agent
    start_agent
done
