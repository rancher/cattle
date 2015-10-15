from common_fixtures import *  # NOQA
from cattle import ApiError

SP_CREATE = "storagepool.create"
VOLUME_CREATE = "volume.create"
VOLUME_DELETE = "volume.delete"


@pytest.fixture(scope='module')
def host(super_client, context):
    return super_client.reload(context.host)


def agent_cli(context):
    return context.agent_client


def from_context(context):
    return context.client, context.agent_client, context.host


def test_external_volume_event(super_client, new_context):
    client, agent_client, host = from_context(new_context)
    sp_ex_id = random_str()
    sp_name = 'name-%s' % sp_ex_id
    external_id = random_str()

    # Create a storage pool for the volume
    create_sp_event(client, agent_client, super_client, new_context,
                    sp_ex_id, sp_name, SP_CREATE, [host.uuid])
    storage_pool = wait_for(lambda: sp_wait(client, sp_ex_id))

    driver = 'convoy'
    uri = '/foo/bar'
    fmt = 'docker'
    is_hp = False

    create_volume_event(client, agent_client, super_client, new_context,
                        sp_ex_id, VOLUME_CREATE, external_id,
                        driver=driver, fmt=fmt, is_hp=is_hp, uri=uri)

    volume = wait_for(lambda: volume_wait(client, external_id))
    volume = wait_for(lambda: volume_in_sp(client, volume, storage_pool))
    assert volume.state == 'inactive'
    assert volume.externalId == external_id
    assert volume.name == external_id
    assert volume.driver == driver
    assert volume.uri == uri
    assert volume.isHostPath == is_hp
    super_volume = super_client.by_id('volume', volume.id)
    assert super_volume.deviceNumber == -1
    assert super_volume.format == fmt

    # Send event again to ensure two volumes are not created
    create_volume_event(client, agent_client, super_client, new_context,
                        sp_ex_id, VOLUME_CREATE, external_id,
                        driver=driver, fmt=fmt, is_hp=is_hp, uri=uri)
    volumes = client.list_volume(externalId=external_id)
    assert len(volumes) == 1

    # Delete volume event
    create_volume_event(client, agent_client, super_client, new_context,
                        sp_ex_id, VOLUME_DELETE, external_id,
                        driver=driver, fmt=fmt, is_hp=is_hp, uri=uri)

    volume = client.wait_success(volume)
    assert volume.state == 'removed'


def test_external_storage_pool_event(super_client, new_context):
    client, agent_client, host = from_context(new_context)

    external_id = random_str()
    name = 'name-%s' % external_id

    # Create a new storage pool with a single host
    uuids = [host.uuid]
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id, name, SP_CREATE, uuids)
    storage_pool = wait_for(lambda: sp_wait(client, external_id))
    assert storage_pool.state == 'active'
    assert storage_pool.externalId == external_id
    assert storage_pool.name == name
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert len(hosts) == 1
    assert hosts[0].uuid == host.uuid

    # Send event again to ensure a second storage pool is not created
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id, name, SP_CREATE, uuids)

    # Add a second host
    host2 = register_simulated_host(new_context)
    uuids.append(host2.uuid)
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 2))
    host_ids = [h.id for h in hosts]
    assert host.id in host_ids
    assert host2.id in host_ids

    # Remove a host
    uuids.pop(0)
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert host2.id in hosts[0].id

    # Send empty host list
    uuids = []
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 0))
    assert len(hosts) == 0


def test_bad_agent(super_client, host):
    # Even though super_client will have permissions to create the container
    # event, additional logic should assert that the creator is a valid agent.
    with pytest.raises(ApiError) as e:
        external_id = random_str()
        super_client.create_external_storage_pool_event(
            externalId=external_id,
            eventType=SP_CREATE,
            hostUuids=[],
            storagePool={
                'name': 'name-%s' % external_id,
                'externalId': external_id,
            })
    assert e.value.error.code == 'MissingRequired'


def create_volume_event(client, agent_client, super_client, context,
                        sp_ex_id, event_type, external_id, driver=None,
                        fmt=None, is_hp=False, uri=None):
    vol_event = {
        'externalId': external_id,
        'eventType': event_type,
        'storagePoolExternalId': sp_ex_id,
        'volume': {
            "externalId": external_id,
            "name": external_id,
            "driver": driver,
            "uri": uri,
            "format": fmt,
            "isHostPath": is_hp,
        }
    }

    event = agent_client.create_external_volume_event(vol_event)
    assert event.externalId == external_id
    assert event.eventType == event_type
    agent_account_id = super_client.reload(event).accountId
    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == agent_account_id

    return event


def create_sp_event(client, agent_client, super_client, context, external_id,
                    name, event_type, host_uuids):
    event = agent_client.create_external_storage_pool_event(
        externalId=external_id,
        eventType=event_type,
        hostUuids=host_uuids,
        storagePool={
            'name': name,
            'externalId': external_id,
        })

    assert event.externalId == external_id
    assert event.eventType == event_type
    assert event.hostUuids == host_uuids
    agent_account_id = super_client.reload(event).accountId

    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == agent_account_id

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
