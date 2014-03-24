#!/bin/bash

source handler.sh

event_handler_resource_change()
{
    info "state=$(event_field data.resource.state)"
}

event_main --no-print-messages
