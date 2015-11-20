from common_fixtures import *  # NOQA
from cattle import ClientApiError

SP_CREATE = "storagepool.create"
VOLUME_CREATE = "volume.create"
VOLUME_DELETE = "volume.delete"


def more_hosts(context):
    host2 = register_simulated_host(context)
    host3 = register_simulated_host(context)
    return context.host, host2, host3


def from_context(context):
    return context.client, context.agent_client, context.host


def add_storage_pool(context, host_uuids=None):
    client, agent_client, host = from_context(context)
    sp_name = 'convoy-%s' % random_str()
    if not host_uuids:
        host_uuids = [host.uuid]

    create_sp_event(client, agent_client, context,
                    sp_name, sp_name, SP_CREATE, host_uuids, sp_name)
    storage_pool = wait_for(lambda: sp_wait(client, sp_name))
    assert storage_pool.state == 'active'
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


def test_finding_shared_volumes(new_context):
    # Tests that when a named is specified in dataVolumes and a volume of
    # that name already exists in a shared storage pool, the pre-existing
    # volume is used
    client, agent_client, host = from_context(new_context)
    storage_pool = add_storage_pool(new_context)
    sp_name = storage_pool.name
    name = random_str()
    uri = '/foo/bar'
    create_volume_event(client, agent_client, new_context, VOLUME_CREATE,
                        name, driver=sp_name, uri=uri)
    volume = wait_for(lambda: volume_wait(client, name))
    volume = wait_for(lambda: volume_in_sp(client, volume, storage_pool))

    path = '/container/path'

    # Previously created volume should show up in dataVolumeMounts
    data_volumes = ['%s:%s' % (name, path)]
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumes=data_volumes)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.dataVolumeMounts[path] == volume.id

    # If volumeDriver == local, should not use the shared volume
    c = client.create_container(imageUuid=new_context.image_uuid,
                                volumeDriver='local',
                                dataVolumes=data_volumes)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert not c.dataVolumeMounts

    # Create another storage pool and add a volume of the same name to it
    storage_pool = add_storage_pool(new_context)
    sp_name2 = storage_pool.name
    uri = '/foo/bar'
    create_volume_event(client, agent_client, new_context, VOLUME_CREATE,
                        name, driver=sp_name2, uri=uri)
    volume2 = wait_for(lambda: volume_in_sp_by_name_wait(name, storage_pool))
    assert volume2.id != volume.id

    # Container should not create successfully because name is ambiguous
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumes=data_volumes)
    with pytest.raises(ClientApiError):
        client.wait_success(c)

    # Container should work if the volume driver is specified
    # Also, throw in testing that an extra non-named volume sticks around
    data_volumes.append('/tmp:/tmp')
    c = client.create_container(imageUuid=new_context.image_uuid,
                                volumeDriver=sp_name2,
                                dataVolumes=data_volumes)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert len(c.dataVolumeMounts) == 1
    assert len(c.dataVolumes) == 2
    assert c.dataVolumeMounts[path] == volume2.id


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


def test_volume_create(new_context):
    client, agent_client, host = from_context(new_context)
    storage_pool = add_storage_pool(new_context)
    sp_name = storage_pool.name
    add_storage_pool(new_context)

    # Create a volume with a driver that points to a storage pool
    v1 = client.create_volume(name=random_str(), driver=sp_name)
    v1 = client.wait_success(v1)
    assert v1.state == 'requested'

    # Create a volume with a driver that cattle doesn't know about
    v2 = client.create_volume(name=random_str(), driver='driver-%s' %
                                                        random_str())
    v2 = client.wait_success(v2)
    assert v2.state == 'requested'

    data_volume_mounts = {'/con/path': v1.id,
                          '/con/path2': v2.id}
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumeMounts=data_volume_mounts)
    c = client.wait_success(c)
    assert c.state == 'running'

    v1 = client.wait_success(v1)
    assert v1.state == 'active'
    sps = v1.storagePools()
    assert len(sps) == 1
    assert sps[0].id == storage_pool.id

    v2 = client.wait_success(v2)
    assert v2.state == 'active'
    sps = v2.storagePools()
    assert len(sps) == 1
    assert sps[0].kind == 'sim'

    # Create a new volume, assign to container via dataVolumes
    # Should be translated to a dataVolumeMount entry.
    v3 = client.create_volume(name=random_str(), driver=sp_name)
    v3 = client.wait_success(v3)
    assert v3.state == 'requested'

    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumes=['%s:/foo' % v3.name])
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.dataVolumeMounts['/foo'] == v3.id
    v3 = client.wait_success(v3)
    assert v3.state == 'active'
    sps = v3.storagePools()
    assert len(sps) == 1
    assert sps[0].id == storage_pool.id

    # Create a new volume, assign to container via dataVolumes, also set
    # volumeDriver in container. Should be translated to a dataVolumeMount
    # entry.
    v4 = client.create_volume(name=random_str(), driver=sp_name)
    v4 = client.wait_success(v4)
    assert v4.state == 'requested'

    c = client.create_container(imageUuid=new_context.image_uuid,
                                volumeDriver=sp_name,
                                dataVolumes=['%s:/foo' % v4.name])
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.dataVolumeMounts['/foo'] == v4.id
    v4 = client.wait_success(v4)
    assert v4.state == 'active'
    sps = v4.storagePools()
    assert len(sps) == 1
    assert sps[0].id == storage_pool.id


def test_volume_lookup_not_local(new_context):
    # When looking up named volumes for scheduling purposes, local volumes
    # should be ignored.
    client = new_context.client
    name = random_str()
    v1 = client.create_volume(name=name, driver='local')
    v1 = client.wait_success(v1)
    c = client.create_container(imageUuid=new_context.image_uuid,
                                dataVolumes=['%s:/foo' % v1.name])
    c = client.wait_success(c)
    assert c.state == 'running'

    c2 = client.create_container(imageUuid=new_context.image_uuid,
                                 dataVolumes=['%s:/foo' % v1.name])
    c2 = client.wait_success(c2)
    assert c2.state == 'running'
    assert not c2.dataVolumeMounts


def test_volume_create_failed_allocation(new_context):
    client, agent_client, host = from_context(new_context)
    storage_pool = add_storage_pool(new_context)
    sp_name = storage_pool.name
    add_storage_pool(new_context)

    v1 = client.wait_success(client.create_volume(name=random_str(),
                                                  driver=sp_name))
    assert v1.state == 'requested'

    # Will fail because new_host is not in the storage_pool that v1 belongs to
    new_host = register_simulated_host(new_context)
    data_volume_mounts = {'/con/path': v1.id}
    with pytest.raises(ClientApiError) as e:
        c = client.create_container(imageUuid=new_context.image_uuid,
                                    requestedHostId=new_host.id,
                                    dataVolumeMounts=data_volume_mounts)
        client.wait_success(c)
    assert e.value.message == 'Failed to find a placement'

    # Put two volumes from mutually exclusive storage pools onto a container
    # and it should fail to find placement
    sp2 = add_storage_pool(new_context, [new_host.uuid])
    v2 = client.create_volume(name=random_str(), driver=sp2.name)
    v2 = client.wait_success(v2)
    assert v1.state == 'requested'
    data_volume_mounts['/con/path2'] = v2.id
    with pytest.raises(ClientApiError) as e:
        c = client.create_container(imageUuid=new_context.image_uuid,
                                    dataVolumeMounts=data_volume_mounts)
        client.wait_success(c)
    assert e.value.message == 'Failed to find a placement'


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


def volume_in_sp_by_name_wait(name, storage_pool):
    volumes = storage_pool.volumes(name=name)
    if len(volumes) and volumes[0].state == 'inactive':
        return volumes[0]


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
