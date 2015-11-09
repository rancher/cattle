from common_fixtures import *  # NOQA
from cattle import ApiError


SP_CREATE = "storagepool.create"
VOLUME_CREATE = "volume.create"
VOLUME_DELETE = "volume.delete"
SERVICE_KIND = 'testservicekind'


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
                    sp_ex_id, sp_name, SP_CREATE, [host.uuid], None)
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
    driver_name = 'rancher-bob'

    # Create a new storage pool with a single host
    uuids = [host.uuid]
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id, name, SP_CREATE, uuids, driver_name)
    storage_pool = wait_for(lambda: sp_wait(client, external_id))
    assert storage_pool.state == 'active'
    assert storage_pool.externalId == external_id
    assert storage_pool.name == name
    assert storage_pool.driverName == driver_name
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert len(hosts) == 1
    assert hosts[0].uuid == host.uuid

    # Send event again to ensure a second storage pool is not created
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id, name, SP_CREATE, uuids, None)

    # Add a second host
    host2 = register_simulated_host(new_context)
    uuids.append(host2.uuid)
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids, None)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 2))
    host_ids = [h.id for h in hosts]
    assert host.id in host_ids
    assert host2.id in host_ids

    # Remove a host
    uuids.pop(0)
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids, None)
    hosts = wait_for(lambda: wait_host_count(storage_pool, 1))
    assert host2.id in hosts[0].id

    # Send empty host list
    uuids = []
    create_sp_event(client, agent_client, super_client, new_context,
                    external_id,
                    name, SP_CREATE, uuids, None)
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


def test_external_host_event_miss(new_context):
    new_context.create_container()

    client = new_context.client
    host = new_context.host
    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate',
                                              deleteHost=True)
    event = client.wait_success(event)
    host = client.reload(host)

    assert event.state == 'created'
    assert host.state == 'active'


def test_external_host_event_wrong_event(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.notevacuate',
                                              deleteHost=True)
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'active'
    assert c.state == 'running'


def test_external_host_event_hit(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    }, deleteHost=True)
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate',
                                              deleteHost=True)
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'purged'
    assert c.state == 'removed'


def test_external_host_event_no_delete(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate')
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'inactive'


def test_external_host_event_by_id(new_context):
    c = new_context.create_container()
    new_host = register_simulated_host(new_context)

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostId=host.id,
                                              eventType='host.evacuate')
    assert event.state == 'creating'

    event = client.wait_success(event)
    new_host = client.reload(new_host)
    c = client.wait_success(c)
    host = client.reload(host)

    assert event.state == 'created'
    assert host.state == 'inactive'
    assert new_host.state == 'active'


def test_external_dns_event(super_client, new_context):
    client, agent_client, host = from_context(new_context)

    stack = client.create_environment(name=random_str())
    stack = client.wait_success(stack)
    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    svc1 = client.create_service(name=random_str(),
                                 environmentId=stack.id,
                                 launchConfig=launch_config)
    svc1 = client.wait_success(svc1)

    domain_name1 = "foo.com"
    create_dns_event(client, agent_client, super_client,
                     new_context, svc1.name,
                     stack.name, domain_name1)

    # wait for dns name to be updated
    svc1 = client.reload(svc1)
    assert svc1.fqdn == domain_name1


def create_dns_event(client, agent_client, super_client,
                     context, svc_name1,
                     stack_name, domain_name):
    external_id = random_str()
    event_type = "externalDnsEvent"
    dns_event = {
        'externalId': external_id,
        'eventType': event_type,
        "stackName": stack_name,
        "serviceName": svc_name1,
        "fqdn": domain_name
    }

    event = agent_client.create_external_dns_event(dns_event)
    assert event.externalId == external_id
    assert event.eventType == event_type
    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == context.agent.id

    return event


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
    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == context.agent.id

    return event


def create_sp_event(client, agent_client, super_client, context, external_id,
                    name, event_type, host_uuids, driver_name):
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


def service_wait(client, external_id):
    services = client.list_testservicekind(externalId=external_id)
    if len(services) and services[0].state == 'active':
        return services[0]


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


def test_external_service_event_create(client, context,
                                       create_dynamic_service_type,
                                       super_client):
    agent_client = context.agent_client

    env_external_id = random_str()
    environment = {"name": "foo", "externalId": env_external_id}

    svc_external_id = random_str()
    svc_name = 'svc-name-%s' % svc_external_id
    selector = 'foo=bar1'
    template = {'foo': 'bar'}
    svc_data = {
        'selectorContainer': selector,
        'kind': SERVICE_KIND,
        'name': svc_name,
        'externalId': svc_external_id,
        'template': template,
    }
    event = agent_client.create_external_service_event(
        eventType='service.create',
        environment=environment,
        externalId=svc_external_id,
        service=svc_data,
    )

    event = wait_for(lambda: event_wait(client, event))
    assert event is not None

    svc = wait_for(lambda: service_wait(client, svc_external_id))

    assert svc.externalId == svc_external_id
    assert svc.name == svc_name
    assert svc.kind == SERVICE_KIND
    assert svc.selectorContainer == selector
    assert svc.environmentId is not None
    assert svc.template == template

    envs = client.list_environment(externalId=env_external_id)
    assert len(envs) == 1
    assert envs[0].id == svc.environmentId

    wait_for_condition(client, svc,
                       lambda x: x.state == 'active',
                       lambda x: 'State is: ' + x.state)

    # Update
    new_selector = 'newselector=foo'
    svc_data = {
        'selectorContainer': new_selector,
        'kind': SERVICE_KIND,
        'template': {'foo': 'bar'},
    }
    agent_client.create_external_service_event(
        eventType='service.update',
        environment=environment,
        externalId=svc_external_id,
        service=svc_data,
    )

    wait_for_condition(client, svc,
                       lambda x: x.selectorContainer == new_selector,
                       lambda x: 'Selector is: ' + x.selectorContainer)

    # Delete
    agent_client.create_external_service_event(
        name=svc_name,
        eventType='service.remove',
        externalId=svc_external_id,
        service={'kind': SERVICE_KIND},
    )

    wait_for_condition(client, svc,
                       lambda x: x.state == 'removed',
                       lambda x: 'State is: ' + x.state)


@pytest.fixture
def create_dynamic_service_type(client, context):
    env = client.wait_success(client.create_environment(name='test'))
    service = client.create_service(environmentId=env.id,
                                    name='test',
                                    launchConfig={
                                        'imageUuid': context.image_uuid
                                    },
                                    serviceSchemas={SERVICE_KIND: {
                                        'resourceFields': {
                                            'template': {
                                                'type': 'json',
                                                'create': True,
                                            }
                                        }
                                    }})
    client.wait_success(service)
    client.reload_schema()
