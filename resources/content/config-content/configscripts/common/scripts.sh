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

export DSTACK_AGENT_LOG_FILE=${DSTACK_AGENT_LOG_FILE:-${DSTACK_HOME}/agent.log}
