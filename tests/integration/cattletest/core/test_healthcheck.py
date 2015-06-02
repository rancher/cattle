from common_fixtures import *  # NOQA


def test_health_check_create_instance(super_client, context, client):
    container = context.create_container(healthCheck={
        'port': 80,
    })

    assert container.healthCheck.port == 80

    container = super_client.reload(container)
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = None
    for map in container.hosts()[0].instanceHostMaps():
        c = map.instance()
        if c.agentId is not None:
            agent = c.agent()

    assert agent is not None
    assert hcihm.healthState == 'unhealthy'

    ts = int(time.time())
    se = super_client.create_service_event(accountId=agent.accountId,
                                           externalTimestamp=ts,
                                           reportedHealth='UP',
                                           healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == container.accountId
    assert se.instanceId == container.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'

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

    exposeMap = find_one(service.serviceExposeMaps)
    container = super_client.reload(exposeMap.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = container.hosts()[0].agent()

    assert hcihm.healthState == 'unhealthy'

    ts = int(time.time())
    se = super_client.create_service_event(accountId=agent.accountId,
                                           externalTimestamp=ts,
                                           reportedHealth='Something Bad',
                                           healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == container.accountId
    assert se.instanceId == container.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'

    check = lambda: super_client.reload(container).healthState == 'unhealthy'
    wait_for(check, timeout=5)
    wait_for(lambda: len(service.serviceExposeMaps()) > 1)
