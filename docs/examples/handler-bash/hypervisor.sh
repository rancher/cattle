#!/bin/bash
set -e

source handler.sh

UUID=bash-hv2
UUID_SAVE_FILE=hypervisor_uuid
KIND=bash-hv

set_uuid()
{
    if [ -z "$UUID" ]; then
        if [ -f hypervisor_uuid ]; then
            UUID=$(<$UUID_SAVE_FILE)
        fi
    fi

    if [ -z "$UUID" ]; then
        UUID=$(uuidgen)
        echo $UUID > $UUID_SAVE_FILE
    fi
}

register()
{
    AGENT_ID=$(get ${URL}/agents?uuid=$UUID | jq -r '.data[0].id // ""')
    
    if [ -z "$AGENT_ID" ]; then
        AGENT_ID=$(post ${URL}/agents \
            -F uri='event://'$UUID \
            -F managedConfig=false \
            -F uuid=$UUID \
            -F name="$(hostname) bash" \
            -F kind=$KIND | jq -r '.id // ""')
    fi
}

event_handler_ping()
{
    if [ "$(echo_event | jq -r '.data.options.resources // false')" = "true" ]; then
        reply '{
            "resources": [
                {
                    "kind" : "'$KIND'",
                    "name" : "'$(hostname)' bash",
                    "uuid" : "'${UUID}-host'",
                    "type" : "host"
                },
                {
                    "kind" : "'$KIND'",
                    "name" : "'$(hostname)' pool bash",
                    "uuid" : "'${UUID}-pool'",
                    "hostUuid" : "'${UUID}-host'",
                    "type" : "storagePool"
                }
            ]
        }'
    else
        reply
    fi
}

create_file()
{
    local name=$1
    local dir=${name}-$2
    local file=${dir}/$3

    if [ ! -e $file ]; then
        mkdir -p $dir
        info Creating $file
        echo_event > $file
    fi
}

rm_file()
{
    local name=$1
    local dir=${name}-$2
    local file=${dir}/$3

    if [ -e $file ]; then
        info Deleting $file
        rm -f $file
    fi
}

event_handler_compute_instance_activate()
{
    local host_uuid=$(event_field data.instanceHostMap.host.uuid)
    local instance_uuid=$(event_field data.instanceHostMap.instance.uuid)
    local instance=${host_uuid}/${instance_uuid}

    create_file instance $host_uuid $instance_uuid

    reply
}

event_handler_compute_instance_deactivate()
{
    local host_uuid=$(event_field data.instanceHostMap.host.uuid)
    local instance_uuid=$(event_field data.instanceHostMap.instance.uuid)
    local instance=${host_uuid}/${instance_uuid}

    rm_file instance $host_uuid $instance_uuid

    reply
}

event_handler_storage_image_activate()
{
    local pool_uuid=$(event_field data.imageStoragePoolMap.storagePool.uuid)
    local image_uuid=$(event_field data.imageStoragePoolMap.image.uuid)

    create_file image $pool_uuid $image_uuid

    reply '{
        "image" : {
            "format" : "touch"
        }
    }'
}

event_handler_storage_volume_activate()
{
    local pool_uuid=$(event_field data.volumeStoragePoolMap.storagePool.uuid)
    local volume_uuid=$(event_field data.volumeStoragePoolMap.volume.uuid)

    create_file volume $pool_uuid $volume_uuid

    reply '{
        "volume" : {
            "format" : "touch"
        }
    }'
}

event_handler_storage_volume_deactivate()
{
    reply
}

event_handler_storage_volume_remove()
{
    local pool_uuid=$(event_field data.volumeStoragePoolMap.storagePool.uuid)
    local volume_uuid=$(event_field data.volumeStoragePoolMap.volume.uuid)

    rm_file volume $pool_uuid $volume_uuid

    reply
}

main()
{
    set_uuid
    register
    info Agent ID $AGENT_ID
    event_main --agent-id $AGENT_ID --url http://localhost:8080/v1 "$@"
}

main "$@"
