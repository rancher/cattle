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
      ${CATTLE_HOME}/etc/cattle/agent/bootstrap.conf
      /var/lib/rancher/etc/agent.conf)
CONTENT_URL=/configcontent/configscripts
INSTALL_ITEMS="configscripts pyagent"
REQUIRED_IMAGE=
DETECTED_CATTLE_AGENT_IP=

export CATTLE_AGENT_IP=${CATTLE_AGENT_IP:-${DETECTED_CATTLE_AGENT_IP}}

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

ca_cert()
{
    mkdir -p /usr/share/ca-certificates/rancher
    cat > /usr/share/ca-certificates/rancher/agent-ca.crt << EOF
%CERT%
EOF
    if ! grep -q rancher/agent-ca.crt /etc/ca-certificates.conf; then
        echo rancher/agent-ca.crt >> /etc/ca-certificates.conf
    fi

    update-ca-certificates
}

start_agent()
{
    local main=/usr/bin/agent.sh
    export AGENT_PARENT_PID=$PPID
    info Starting agent $main
    exec $main
}

print_config()
{
    info Access Key: $CATTLE_ACCESS_KEY
    info Config URL: $CATTLE_CONFIG_URL
    info Storage URL: $CATTLE_STORAGE_URL
    info API URL: $CATTLE_URL
    info IP: $CATTLE_AGENT_IP
    info Port: $CATTLE_AGENT_PORT
    info Required Image: ${REQUIRED_IMAGE}
    info Current Image: ${RANCHER_AGENT_IMAGE}
}

upgrade()
{
    if [ "${CATTLE_SKIP_UPGRADE}" = "true" ]; then
        return 0
    fi

    if [[ -n "${REQUIRED_IMAGE}" && "${RANCHER_AGENT_IMAGE}" != "${REQUIRED_IMAGE}" ]]; then
        if [ -e /host/var/run/docker.sock ]; then
            # Upgrading from old image
            export DOCKER_HOST="unix:///host/var/run/docker.sock"
        fi

        info Upgrading to image ${REQUIRED_IMAGE}

        while docker inspect rancher-agent-upgrade >/dev/null 2>&1; do
            docker rm -f rancher-agent-upgrade
            sleep 1
        done

        docker run -d --privileged --name rancher-agent-upgrade -v /var/run/docker.sock:/var/run/docker.sock ${REQUIRED_IMAGE} upgrade
        exit 0
    elif [ -n "${REQUIRED_IMAGE}" ]; then
        info Using image ${REQUIRED_IMAGE}
    fi
}

get_running_image()
{
    echo $(docker inspect -f '{{.Config.Image}}' rancher-agent)
}

cd $(dirname $0)

for conf_file in "${CONF[@]}"; do
    if [ -e $conf_file ]
    then
        source $conf_file
    fi
done

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
export RANCHER_AGENT_IMAGE="$(get_running_image)"
print_config

upgrade

ca_cert
start_agent
