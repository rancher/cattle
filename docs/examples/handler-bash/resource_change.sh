#!/bin/bash

source handler.sh

# The handler.sh framework will find this method dynamically and register
# an event listener which will be the equivalent to
#
#    curl -X POST http://localhost:8080/v1/subscribe -F eventNames=resource.change
#
event_handler_resource_change()
{
    info "state=$(event_field data.resource.state)"
}

event_main --no-print-messages
