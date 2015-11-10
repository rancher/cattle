from common_fixtures import *  # NOQA

SP_CREATE = "storagepool.create"
VOLUME_CREATE = "volume.create"
VOLUME_DELETE = "volume.delete"


def more_hosts(context):
    host2 = register_simulated_host(context)
    host3 = register_simulated_host(context)
    return context.host, host2, host3


def from_context(context):
    return context.client, context.agent_client, context.host


def add_storage_pool(context):
    client, agent_client, host = from_context(context)
    sp_name = 'convoy-%s' % random_str()
    uuids = [host.uuid]
    create_sp_event(client, agent_client, context,
                    sp_name, sp_name, SP_CREATE, uuids, sp_name)
    storage_pool = wait_for(lambda: sp_wait(client, sp_name))
    assert storage_pool.state == 'active'
    host = client.reload(host)
    storage_pools = host.storagePools()
    assert len(storage_pools) == 2
    return storage_pool


def test_multiple_sp_volume_schedule(new_context):
    # Tests that when a host has more than one storage pool (one local, one
    # shared), and a container is scheduled to it, the root volume can be
    # properly scheduled.
    client = new_context.client
    add_storage_pool(new_context)

    # The allocation bug that caused this issue is much more likely to occur
    # when two containers are created back-to-back
    c = client.create_container(imageUuid=new_context.image_uuid,
                                networkMode=None)
    c2 = client.create_container(imageUuid=new_context.image_uuid,
                                 networkMode=None)

    c = client.wait_success(c)
    assert c is not None
    vols = c.volumes()
    assert len(vols) == 1
    vol_pools = vols[0].storagePools()
    assert len(vol_pools) == 1
    assert vol_pools[0].kind == 'sim'

    c2 = client.wait_success(c2)
    assert c2 is not None
    vols = c2.volumes()
    assert len(vols) == 1
    vol_pools = vols[0].storagePools()
    assert len(vol_pools) == 1
    assert vol_pools[0].kind == 'sim'


def test_data_volume_mounts(new_context):
    client, agent_client, host = from_context(new_context)
    storage_pool = add_storage_pool(new_context)
    sp_name = storage_pool.name
    external_id = random_str()
    uri = '/foo/bar'
    create_volume_event(client, agent_client, new_context, VOLUME_CREATE,
                        external_id, driver=sp_name, uri=uri)
    volume = wait_for(lambda: volume_wait(client, external_id))
    volume = wait_for(lambda: volume_in_sp(client, volume, storage_pool))

    data_volume_mounts = {'/somedir': volume.id}
    c = client.create_container(imageUuid=new_context.image_uuid,
                                volumeDriver='local',
                                dataVolumeMounts=data_volume_mounts)
    c = client.wait_success(c, timeout=240)
    assert c.state == 'running'
    assert c.dataVolumes[0] == '%s:/somedir' % external_id


def test_external_volume_event(super_client, new_context):
    client, agent_client, host = from_context(new_context)
    storage_pool = add_storage_pool(new_context)
    sp_name = storage_pool.name
    external_id = random_str()
    uri = '/foo/bar'

    create_volume_event(client, agent_client, new_context, VOLUME_CREATE,
                        external_id, driver=sp_name, uri=uri)

    volume = wait_for(lambda: volume_wait(client, external_id))
    volume = wait_for(lambda: volume_in_sp(client, volume, storage_pool))
    assert volume.state == 'inactive'
    assert volume.externalId == external_id
    assert volume.name == external_id
    assert volume.driver == sp_name
    assert volume.uri == uri
    assert volume.isHostPath is False
    super_volume = super_client.by_id('volume', volume.id)
    assert super_volume.deviceNumber == -1
    assert super_volume.format == 'docker'

    # Send event again to ensure two volumes are not created
    create_volume_event(client, agent_client, new_context,
                        VOLUME_CREATE, external_id, driver=sp_name, uri=uri)
    volumes = client.list_volume(externalId=external_id)
    assert len(volumes) == 1

    # Delete volume event
    create_volume_event(client, agent_client, new_context, VOLUME_DELETE,
                        external_id, driver=sp_name, uri=uri)

    volume = client.wait_success(volume)
    assert volume.state == 'removed'


def test_external_storage_pool_event(new_context):
    client, agent_client, host = from_context(new_context)
    sp_name = 'convoy-%s' % random_str()

    # Create a new storage pool with a single host
    uuids = [host.uuid]
    create_sp_event(client, agent_client, new_context,
                    sp_name, sp_name, SP_CREATE, uuids, sp_name)
    storage_pool = wait_for(lambda: sp_wait(client, sp_name))
    assert storage_pool.state == 'active'
    assert storage_pool.externalId == sp_name
    assert storage_pool.name == sp_name
    assert storage_pool.driverName == sp_name
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert len(hosts) == 1
    assert hosts[0].uuid == host.uuid

    # Send event again to ensure a second storage pool is not created
    create_sp_event(client, agent_client, new_context,
                    sp_name, sp_name, SP_CREATE, uuids, sp_name)

    # Add a second host
    host2 = register_simulated_host(new_context)
    uuids.append(host2.uuid)
    create_sp_event(client, agent_client, new_context,
                    sp_name,
                    sp_name, SP_CREATE, uuids, sp_name)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 2))
    host_ids = [h.id for h in hosts]
    assert host.id in host_ids
    assert host2.id in host_ids

    # Remove a host
    uuids.pop(0)
    create_sp_event(client, agent_client, new_context,
                    sp_name,
                    sp_name, SP_CREATE, uuids, sp_name)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert host2.id in hosts[0].id

    # Send empty host list
    uuids = []
    create_sp_event(client, agent_client, new_context,
                    sp_name,
                    sp_name, SP_CREATE, uuids, sp_name)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 0))
    assert len(hosts) == 0


def create_volume_event(client, agent_client, context, event_type,
                        external_id, driver=None, uri=None):
    vol_event = {
        'externalId': external_id,
        'eventType': event_type,
        'volume': {
            'externalId': external_id,
            'name': external_id,
            'driver': driver,
            'uri': uri,
            'format': 'docker',
            'isHostPath': False,
        }
    }

    event = agent_client.create_external_volume_event(vol_event)
    assert event.externalId == external_id
    assert event.eventType == event_type
    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == context.agent.id

    return event


def create_sp_event(client, agent_client, context, external_id, name,
                    event_type, host_uuids, driver_name):
    event = agent_client.create_external_storage_pool_event(
        externalId=external_id,
        eventType=event_type,
        hostUuids=host_uuids,
        storagePool={
            'name': name,
            'externalId': external_id,
            'driverName': driver_name,
        })

    assert event.externalId == external_id
    assert event.eventType == event_type
    assert event.hostUuids == host_uuids

    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == context.agent.id

    return event


def sp_wait(client, external_id):
    storage_pools = client.list_storage_pool(externalId=external_id)
    if len(storage_pools) and storage_pools[0].state == 'active':
        return storage_pools[0]


def volume_wait(client, external_id):
    volumes = client.list_volume(externalId=external_id)
    if len(volumes) and volumes[0].state == 'inactive':
        return volumes[0]


def wait_host_count(storage_pool, count):
    new_hosts = storage_pool.hosts()
    if len(new_hosts) == count:
        return new_hosts


def volume_in_sp(client, volume, storage_pool):
    volumes = storage_pool.volumes()
    if len(volumes) > 0:
        for v in volumes:
            if v.id == volume.id:
                return volume


def event_wait(client, event):
    created = client.by_id('externalEvent', event.id)
    if created is not None and created.state == 'created':
        return created
