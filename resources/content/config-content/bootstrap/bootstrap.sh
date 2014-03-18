#!/bin/bash
set -e

trap cleanup EXIT

# This is copied from common/scripts.sh, if there is a change here
# make it in common and then copy here
check_debug()
{
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

export DSTACK_HOME=${DSTACK_HOME:-/var/lib/dstack}

check_debug
# End copy

CONF=(/etc/dstack/agent/bootstrap.conf
      /var/lib/dstack/etc/dstack/agent/bootstrap.conf)
CONTENT_URL=/configcontent/configscripts
INSTALL_ITEMS="configscripts pyagent"
DOCKER_AGENT="ibuildthecloud/agent"

cleanup()
{
    if [ -e "$TEMP_DOWNLOAD" ]; then
        rm -rf $TEMP_DOWNLOAD
    fi

    if [ -e $0 ] && echo $0 | grep -q ^/tmp
    then
        rm $0 2>/null || true
    fi
}

cleanup_docker()
{
    for d in $(docker ps -a | awk '{print $1 " " $2}' | grep $DOCKER_AGENT | awk '{print $1}'); do
        echo Cleaning up $d
        docker kill $d 2>/dev/null | true
        docker rm $d 2>/dev/null | true
    done
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
    info IP: $DSTACK_AGENT_IP
    info Port: $DSTACK_AGENT_PORT
}


cd $(dirname $0)

for conf_file in "${CONF[@]}"; do
    if [ -e $conf_file ]
    then
        source $conf_file
    fi
done

if [ "$INCEPTION" = "true" ] && [ "$INCEPTION_INCEPTION" = "" ] && [ -e /proc-host/1/ns/net ]; then
    export INCEPTION_INCEPTION=true
    exec /usr/sbin/nsenter --net=/proc-host/1/ns/net --uts=/proc-host/1/ns/uts -F -- $0 "$@"
fi

if [ "$DSTACK_AGENT_INCEPTION" = "true" ] || ! python -V >/dev/null 2>&1; then
    if [ "$INCEPTION" != "true" ]; then
        cleanup_docker
        exec docker run -rm -privileged -i -w $(pwd) \
                    -v /run:/run \
                    -v /var:/var \
                    -v /proc:/proc-host \
                    -v $(pwd):$(pwd) \
                    -v $(readlink -f /var/lib/docker):$(readlink -f /var/lib/docker) \
                    -e DSTACK_SCRIPT_DEBUG=$DSTACK_SCRIPT_DEBUG \
                    -e INCEPTION=true \
                    $DSTACK_DOCKER_AGENT_ARGS $DOCKER_AGENT $0 "$@"
    fi
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

    eval "$LINE"
    check_debug

    while [ $# != 0 ]; do
        case $1 in
        --port)
            shift 1
            if [ -z "$DSTACK_AGENT_PORT" ];then
                export DSTACK_AGENT_PORT=$1
            fi
        ;;
        --ip)
            shift 1
            if [ -z "$DSTACK_AGENT_IP" ];then
                export DSTACK_AGENT_IP=$1
            fi
        ;;
        esac

        shift 1
    done

    print_config

    mkdir -p $DSTACK_HOME
    download_agent
    start_agent
done
