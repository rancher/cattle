#!/bin/bash
set -e

trap cleanup EXIT SIGINT SIGTERM

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

CONF=(/etc/cattle/agent/bootstrap.conf
      ${CATTLE_HOME}/etc/cattle/agent/bootstrap.conf)
CONTENT_URL=/configcontent/configscripts
INSTALL_ITEMS="configscripts pyagent"
DOCKER_AGENT="cattle/agent"

cleanup()
{
    local exit=$?

    if [ -e "$TEMP_DOWNLOAD" ]; then
        rm -rf $TEMP_DOWNLOAD
    fi

    if [ -e $0 ] && echo $0 | grep -q ^/tmp
    then
        rm $0 2>/null || true
    fi

    return $exit
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
    info Downloading agent "${CATTLE_CONFIG_URL}${CONTENT_URL}"
    curl --retry 5 -s -u $CATTLE_ACCESS_KEY:$CATTLE_SECRET_KEY ${CATTLE_CONFIG_URL}${CONTENT_URL} > $TEMP_DOWNLOAD/content
    tar xzf $TEMP_DOWNLOAD/content -C $TEMP_DOWNLOAD || ( cat $TEMP_DOWNLOAD/content 1>&2 && exit 1 )
    bash $TEMP_DOWNLOAD/*/config.sh --force $INSTALL_ITEMS
}

start_agent()
{
    local main=${CATTLE_HOME}/pyagent/apply.sh
    export AGENT_PARENT_PID=$$
    info Starting agent $main
    $main start
}

print_config()
{
    info Access Key: $CATTLE_ACCESS_KEY
    info Config URL: $CATTLE_CONFIG_URL
    info Storage URL: $CATTLE_STORAGE_URL
    info API URL: $CATTLE_URL
    info IP: $CATTLE_AGENT_IP
    info Port: $CATTLE_AGENT_PORT
}

break_out_of_docker()
{
    if [ "$CATTLE_INSIDE_DOCKER" = "true" ] && [ -e /host/proc/1/ns/net ] && [ -e /host/proc/1/ns/uts ]; then
        exec env CATTLE_INSIDE_DOCKER=outside /usr/sbin/nsenter --net=/host/proc/1/ns/net --uts=/host/proc/1/ns/uts -F -- "$@"
    fi
}

handle_inception()
{
    if [ "$CATTLE_INSIDE_DOCKER" != "outside" ]; then
        if [ "$CATTLE_AGENT_INCEPTION" = "true" ] || ! python -V >/dev/null 2>&1; then
            cleanup_docker
            exec docker run -rm -privileged -i -w $(pwd) \
                        -v /run:/host/run \
                        -v /var:/host/var \
                        -v /proc:/host/proc \
                        -e CATTLE_EXEC_AGENT=true \
                        -e CATTLE_SCRIPT_DEBUG=$CATTLE_SCRIPT_DEBUG \
                        $CATTLE_DOCKER_AGENT_ARGS $DOCKER_AGENT "$@"
        fi
    fi
}

cd $(dirname $0)

for conf_file in "${CONF[@]}"; do
    if [ -e $conf_file ]
    then
        source $conf_file
    fi
done

break_out_of_docker $0 "$@"
handle_inception "$@"

while [ $# != 0 ]; do
    case $1 in
    --port)
        shift 1
        if [ -z "$CATTLE_AGENT_PORT" ];then
            export CATTLE_AGENT_PORT=$1
        fi
        ;;
    --ip)
        shift 1
        if [ -z "$CATTLE_AGENT_IP" ];then
            export CATTLE_AGENT_IP=$1
        fi
        ;;
    --read-env)
        read LINE
        eval "$LINE"
        ;;
    esac

    shift 1
done

check_debug
print_config

mkdir -p $CATTLE_HOME
download_agent
start_agent
