from common_fixtures import *  # NOQA
from cattle import ApiError


def _get_agent_for_container(container):
    agent = None
    for map in container.hosts()[0].instanceHostMaps():
        c = map.instance()
        if c.agentId is not None:
            agent = c.agent()

    assert agent is not None
    return agent


def _get_agent_client(agent):
    creds = agent.account().credentials()

    api_key = [x for x in creds if x.kind == 'agentApiKey'][0]
    assert len(api_key)
    return api_client(api_key.publicValue, api_key.secretValue)


def test_health_check_create_instance(super_client, context):
    container = context.create_container(healthCheck={
        'port': 80,
    })

    assert container.healthCheck.port == 80

    container = super_client.reload(container)
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(container)

    assert hcihm.healthState == 'healthy'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == container.accountId
    assert se.instanceId == container.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    assert hcihm.externalTimestamp == ts

    check = lambda: super_client.reload(container).healthState == 'healthy'
    wait_for(check, timeout=5)


def test_health_check_create_service(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id, scale=1)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(container)

    assert hcihm.healthState == 'healthy'
    assert container.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    assert container.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    check = lambda: super_client.reload(container).healthState == 'healthy'
    wait_for(check, timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == container.accountId
    assert se.instanceId == container.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'
    assert hcihm.externalTimestamp == ts

    check = lambda: super_client.reload(container).healthState == 'unhealthy'
    wait_for(check, timeout=5)
    wait_for(lambda: len(service.serviceExposeMaps()) > 1)


def test_health_check_bad_external_timestamp(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id, scale=1)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(container)
    agent_client = _get_agent_client(agent)

    assert hcihm.healthState == 'healthy'

    with pytest.raises(ApiError) as e:
        agent_client.create_service_event(reportedHealth='Something Bad',
                                          healthcheckUuid=hcihm.uuid)
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'externalTimestamp'


def test_health_check_bad_agent(super_client, context, client):
    # Create another host to get the agent from that host
    host2 = super_client.reload(register_simulated_host(context))
    # register one more host to ensure
    # there is at least one more host
    #  to schedule healtcheck on
    super_client.reload(register_simulated_host(context))

    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id, scale=1)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = None
    for h in hci.healthcheckInstanceHostMaps():
        if h.hostId != host2.id:
            hcihm = h
            break

    assert hcihm.hostId != host2.id
    agent_client = _get_agent_client(host2.agent())

    assert hcihm.healthState == 'healthy'

    ts = int(time.time())
    with pytest.raises(ApiError) as e:
        agent_client.create_service_event(externalTimestamp=ts,
                                          reportedHealth='Something Bad',
                                          healthcheckUuid=hcihm.uuid)
    assert e.value.error.code == 'CantVerifyHealthcheck'


def test_health_check_host_remove(super_client, context, client):
    # create 4 hosts for healtcheck as one of them would be removed later
    super_client.reload(register_simulated_host(context))
    super_client.reload(register_simulated_host(context))
    super_client.reload(register_simulated_host(context))
    super_client.reload(register_simulated_host(context))

    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id, scale=1)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    assert len(hci.healthcheckInstanceHostMaps()) == 3

    hcihm = hci.healthcheckInstanceHostMaps()[0]
    hosts = super_client.list_host(uuid=hcihm.host().uuid)
    assert len(hosts) == 1
    host = hosts[0]

    # remove the host
    host = super_client.wait_success(host.deactivate())
    host = super_client.wait_success(super_client.delete(host))
    assert host.state == 'removed'

    # verify that new hostmap was created for the instance
    hci = find_one(container.healthcheckInstances)
    assert len(hci.healthcheckInstanceHostMaps()) == 3

    hcim = None
    for h in hci.healthcheckInstanceHostMaps():
        if h.hostId == host.id:
            hcihm = h
            break

    assert hcim is None
