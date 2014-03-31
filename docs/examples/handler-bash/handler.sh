#!/bin/bash
set -e
# This script provides a simple framework for listening to and handling
# events in Cattle.  Refer to resource_change.sh for a simple example of
# this script.  Refer to hypervisor.sh for a more complex example.
#
# This script requires jq, http://stedolan.github.io/jq/.  If you are not
# familiar with jq you should read the docs on how it works.  jq is basically
# like sed and awk for JSON.

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

    # This looks for all defined functions that are of the form
    # event_handler_* and will form the equivalent subscriptions.
    # For example, event_handler_instance_start will turn into a subscription
    # for instance.start.
    #
    # Additionally this will look for registered event handler functions in the
    # EVENT_HANDLERS associative array.  This is useful if you want to subscribe
    # to an event that has a special character in it like ";".  For example,
    # you can do EVENT_HANDLERS["instance.start;handler=demo"]=my_handler_func
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

    # If AGENT_ID env var is set, then pass it as agentId in the POST
    # to /v1/subscribe
    if [ -n "$AGENT_ID" ]; then
        args="-F agentId=$AGENT_ID"
    fi

    for sub in $(subscriptions); do
        args="$args -F eventNames=$sub"
    done

    post -N ${SUB} $args
}

publish()
{
    # POST and echo the response
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
    if [ ! -x "$(which jq)" ]; then
        echo 'Failed to find jq, please download it from http://stedolan.github.io/jq/.'
        exit 1
    fi

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
    # By default just reply to pings
    reply
}
