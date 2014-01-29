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
