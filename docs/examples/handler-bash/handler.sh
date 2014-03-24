#!/bin/bash
set -e

declare -A EVENT_HANDLERS

URL=http://localhost:8080/v1
SUB=${URL}/subscribe
PUB=${URL}/publish
PRINT_PINGS=${PRINT_PINGS:-false}
PRINT_MESSAGES=${PRINT_MESSAGES:-true}
CURL="curl -s"

get()
{
    $CURL "$@"
}

post()
{
    $CURL -X POST "$@"
}

check_debug()
{
    if [ "$DEBUG" = "true" ]; then
        debug_on
    fi
}

debug_on()
{
    export DEBUG=true
    export PS4='[${BASH_SOURCE##*/}:${LINENO}] '
    set -x
}

debug_off()
{
    set +x
    unset PS4
    unset DEBUG
}

subscriptions()
{
    local sub

    for sub in $(
        declare -F | awk '{print $3}' | grep '^event_handler_' | sed \
            -e 's/^event_handler_//' \
            -e 's/_/./g'
        echo ${!EVENT_HANDLERS[@]}
    ); do
        if [ "$sub" = "ping" ]; then
            if [ -n "$AGENT_ID" ]; then
                echo $sub
            fi
        else
            echo $sub
        fi
    done
}

subscribe()
{
    local args
    local sub

    if [ -n "$AGENT_ID" ]; then
        args="-F agentId=$AGENT_ID"
    fi

    for sub in $(subscriptions); do
        args="$args -F eventNames=$sub"
    done
    # stdbuf is to avoid the standard output buffer that gets applied that will
    # cause messages to get delayed because they are stuck in the buffer
    post -N ${SUB} $args
}

publish()
{
    post ${PUB} -H 'Content-Type: application/json' -d @- | jq .
    return ${PIPESTATUS[0]}
}

should_print_reply()
{
    if [ "$PRINT_MESSAGES" != "true" ];then
        return 1
    fi

    local name=$(event_name)
    if [ "${PRINT_PINGS}" = "false" ] && [ "$name" = "ping" ]; then
        return 1
    fi
}

print_request()
{
    local name=$(event_name)
    if [ "${PRINT_PINGS}" = "false" ] && [ "$name" = "ping" ]; then
        return 0
    fi

    info "Request ($name))"
    echo_event | jq .
}

dispatch()
{
    local full_name=$(event_field name)
    local name=$(event_name)

    if [ "$PRINT_MESSAGES" = "true" ]; then
        print_request
    fi

    local handler=${EVENT_HANDLERS[$full_name]}
    if [ -z "$handler" ]; then
        local handler=${EVENT_HANDLERS[$name]}
    fi

    if [ -z "$handler" ]; then
        handler="event_handler_$(echo $name | sed 's/\./_/g')"
    fi

    if [ "$(type -t $handler)" = "function" ]; then
        local debug
        if [ "$DEBUG" = "$handler" ]; then
            debug=$DEBUG
            debug_on
        fi

        if [ "$handler" != "event_handler_ping" ]; then
            info Invoking $handler
        fi
        eval $handler || error Invoking $handler

        if [ "$debug" != "" ]; then
            debug_off
            DEBUG=$debug
        fi
    fi
}

event_main()
{
    check_debug
    while [ "$#" -gt 0 ]; do
        case $1 in
            --agent-id)
                shift 1
                AGENT_ID=$1
                ;;
            --no-print-messages)
                PRINT_MESSAGES=false
                ;;
            --print-pings)
                PRINT_PINGS=true
                ;;
            --url)
                shift 1
                URL=$1
                ;;
        esac
        shift 1
    done

    main_loop
}

main_loop()
{
    local sub

    info Subscribing to:
    for sub in $(subscriptions | sort); do
        info "    " $sub
    done

    while read EVENT; do
        if [ -z "$(echo_event)" ]; then
            continue
        fi
        dispatch
    done < <(subscribe)
}

event_field()
{
    echo_event | jq -r ".${1} // \"\""
}

event_name()
{
    event_field name | sed 's/;.*//g'
}

echo_event()
{
    echo "$EVENT"
}

error()
{
    set +x
    echo "[ERROR] [$(date)] [$(event_field context.prettyProcess)] [$(event_field id)] [$(event_field resourceType):$(event_field resourceId)]" "$@" 1>&2
    check_debug
}

info()
{
    set +x
    echo "[INFO ] [$(date)] [$(event_field context.prettyProcess)] [$(event_field id)] [$(event_field resourceType):$(event_field resourceId)]" "$@"
    check_debug
}

reply()
{
    if [ -z "$(event_field replyTo)" ]; then
        return 0
    fi

    local data=$1

    if [ -z "$data" ]; then
        data='{}'
    fi

    if should_print_reply; then
        info "Response ($(event_name))"
        jq -s '
            .[0] as $event |
            .[1] as $data |
            {
                "id" : "'$(uuidgen)'",
                "time" : '$(($(date '+%s') * 1000))',
                "name" : $event.replyTo,
                "data" : $data,
                "previousNames" : [ $event.name ],
                "previousIds" : [ $event.id ],
                "resourceId" : $event.resourceId,
                "resourcetype" : $event.resourceType
            }
        ' <(echo_event) <(echo "${data}")
    fi

    jq -s '
        .[0] as $event |
        .[1] as $data |
        {
            "id" : "'$(uuidgen)'",
            "time" : '$(($(date '+%s') * 1000))',
            "name" : $event.replyTo,
            "data" : $data,
            "previousNames" : [ $event.name ],
            "previousIds" : [ $event.id ],
            "resourceId" : $event.resourceId,
            "resourcetype" : $event.resourceType
        }
    ' <(echo_event) <(echo "${data}") |  publish
}

json()
{
    jq -n "$@"
}

event_handler_ping()
{
    reply
}
