from common_fixtures import *  # NOQA
from cattle import ApiError
import yaml


def _get_agent_for_container(container):
    agent = None
    for map in container.hosts()[0].instanceHostMaps():
        c = map.instance()
        if c.agentId is not None:
            agent = c.agent()
            break

    assert agent is not None
    return agent


def _get_agent_client(agent):
    creds = agent.account().credentials()

    api_key = [x for x in creds if x.kind == 'agentApiKey'][0]
    assert len(api_key)
    return api_client(api_key.publicValue, api_key.secretValue)


def test_health_check_create_instance(super_client, context):
    c = context.create_container(healthCheck={
        'port': 80,
    })

    assert c.healthCheck.port == 80

    c = super_client.reload(c)
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(c)

    assert hcihm.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == c.accountId
    assert se.instanceId == c.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    assert hcihm.externalTimestamp == ts

    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)


def _create_svc_w_healthcheck(client, context):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id)
    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'
    return service


def test_health_check_create_service(super_client, context, client):
    service = _create_svc_w_healthcheck(client, context)

    expose_map = find_one(service.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='INIT',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    # restart the instance
    c = super_client.wait_success(c.stop())
    wait_for(lambda: super_client.reload(c).state == 'running',
             timeout=5)
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something bad',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='INIT',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == c.accountId
    assert se.instanceId == c.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'
    assert hcihm.externalTimestamp == ts

    wait_for(lambda: super_client.reload(c).healthState == 'unhealthy',
             timeout=5)
    wait_for(lambda: len(service.serviceExposeMaps()) > 1)


def test_health_check_init_timeout(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'initializingTimeout': 1,
        }
    }, environmentId=env.id)
    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'
    h_c = service.launchConfig.healthCheck
    assert h_c.initializingTimeout == 1

    expose_map = find_one(service.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    # wait for the instance to be removed
    wait_for_condition(client, c,
                       lambda x: x.state == 'removed')


def test_health_check_reinit_timeout(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'reinitializingTimeout': 1,
        }
    }, environmentId=env.id)
    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'
    h_c = service.launchConfig.healthCheck
    assert h_c.reinitializingTimeout == 1

    expose_map = find_one(service.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)

    # restart the instance
    c = super_client.wait_success(c.stop())
    wait_for(lambda: super_client.reload(c).state == 'running',
             timeout=5)
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing',
             timeout=5)

    # wait for the instance to be removed
    wait_for_condition(super_client, c,
                       lambda x: x.state == 'removed')


def test_health_check_bad_external_timestamp(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(container)
    agent_client = _get_agent_client(agent)

    assert hcihm.healthState == 'initializing'

    with pytest.raises(ApiError) as e:
        agent_client.create_service_event(reportedHealth='Something Bad',
                                          healthcheckUuid=hcihm.uuid)
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'externalTimestamp'


def test_health_check_noop(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    svc = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'strategy': 'none'
        }
    }, environmentId=env.id)
    svc = client.wait_success(client.wait_success(svc).activate())
    assert svc.state == 'active'
    assert svc.launchConfig.healthCheck.strategy == 'none'

    expose_map = find_one(svc.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    hcihm = _update_healthy(agent, hcihm, c, super_client)

    _update_unhealthy(agent, hcihm, c, super_client)

    svc = super_client.wait_success(svc)
    assert svc.state == "active"
    assert len(svc.serviceExposeMaps()) == 1
    c = super_client.wait_success(c)
    assert c.state == 'running'


def test_health_check_quorum(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    svc = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'recreateOnQuorumStrategyConfig': {"quorum": 2},
            'strategy': "recreateOnQuorum"
        }
    }, environmentId=env.id, scale=2)
    svc = client.wait_success(client.wait_success(svc).activate())
    assert svc.state == 'active'
    action = svc.launchConfig.healthCheck.strategy
    config = svc.launchConfig.healthCheck.recreateOnQuorumStrategyConfig
    assert action == 'recreateOnQuorum'
    assert config.quorum == 2

    expose_maps = svc.serviceExposeMaps()
    c1 = super_client.reload(expose_maps[0].instance())
    hci1 = find_one(c1.healthcheckInstances)
    hcihm1 = find_one(hci1.healthcheckInstanceHostMaps)
    agent1 = _get_agent_for_container(c1)
    assert hcihm1.healthState == 'initializing'
    assert c1.healthState == 'initializing'
    hcihm1 = _update_healthy(agent1, hcihm1, c1, super_client)

    c2 = super_client.reload(expose_maps[1].instance())
    hci2 = find_one(c2.healthcheckInstances)
    hcihm2 = find_one(hci2.healthcheckInstanceHostMaps)
    agent2 = _get_agent_for_container(c2)
    assert hcihm2.healthState == 'initializing'
    assert c2.healthState == 'initializing'
    hcihm2 = _update_healthy(agent2, hcihm2, c2, super_client)

    # update unheatlhy, check container is not removed
    # as quorum is not reached yet
    _update_unhealthy(agent1, hcihm1, c1, super_client)
    svc = super_client.wait_success(svc)
    assert svc.state == "active"
    assert len(svc.serviceExposeMaps()) == 2
    c1 = super_client.wait_success(c1)
    assert c1.state == 'running'

    hcihm1 = _update_healthy(agent1, hcihm1, c1, super_client)
    svc = super_client.wait_success(svc)
    # increase the scale
    # update unheatlhy, check container removed
    # as quorum is reached
    svc = super_client.update(svc, scale=3)
    svc = super_client.wait_success(svc)
    expose_maps = svc.serviceExposeMaps()
    c3 = super_client.reload(expose_maps[2].instance())
    hci3 = find_one(c3.healthcheckInstances)
    hcihm3 = find_one(hci3.healthcheckInstanceHostMaps)
    agent3 = _get_agent_for_container(c3)
    assert hcihm3.healthState == 'initializing'
    assert c3.healthState == 'initializing'
    hcihm3 = _update_healthy(agent3, hcihm3, c3, super_client)

    _update_unhealthy(agent1, hcihm1, c1, super_client)
    svc = super_client.wait_success(svc)
    assert svc.state == "active"
    assert len(svc.serviceExposeMaps()) >= 3
    wait_for_condition(client, c1,
                       lambda x: x.state == 'removed')


def test_health_check_default(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    svc = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80
        }
    }, environmentId=env.id)
    svc = client.wait_success(client.wait_success(svc).activate())
    assert svc.state == 'active'

    expose_maps = svc.serviceExposeMaps()
    c1 = super_client.reload(expose_maps[0].instance())
    hci = find_one(c1.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(c1)

    assert hcihm.healthState == 'initializing'
    assert c1.healthState == 'initializing'

    hcihm = _update_healthy(agent, hcihm, c1, super_client)

    # update unheatlhy, the container should be removed
    _update_unhealthy(agent, hcihm, c1, super_client)
    svc = super_client.wait_success(svc)
    assert svc.state == "active"
    assert len(svc.serviceExposeMaps()) >= 1
    c1 = super_client.wait_success(c1)
    wait_for_condition(client, c1,
                       lambda x: x.state == 'removed')


def test_health_check_bad_agent(super_client, context, client):
    # Create another host to get the agent from that host
    host2 = super_client.reload(register_simulated_host(context))
    # register one more host to ensure
    # there is at least one more host
    # to schedule healtcheck on
    register_simulated_host(context)

    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id)

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

    assert hcihm.healthState == 'initializing'

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
    }, environmentId=env.id)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    multiport = client.create_service(name='manyports', launchConfig={
        'imageUuid': context.image_uuid,
        'ports': "5454"
    }, environmentId=env.id, scale=3)
    multiport = client.wait_success(client.wait_success(multiport).activate())
    assert multiport.state == 'active'

    expose_map = find_one(service.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    initial_len = len(c.healthcheckInstanceHostMaps())
    assert initial_len == 3

    for h in c.healthcheckInstanceHostMaps():
        assert h.healthState == c.healthState

    hcihm = c.healthcheckInstanceHostMaps()[0]
    hosts = super_client.list_host(uuid=hcihm.host().uuid)
    assert len(hosts) == 1
    host = hosts[0]

    # remove the host
    host = super_client.wait_success(host.deactivate())
    host = super_client.wait_success(super_client.delete(host))
    assert host.state == 'removed'

    # verify that new hostmap was created for the instance
    final_len = len(c.healthcheckInstanceHostMaps())
    assert final_len >= initial_len

    hcim = None
    for h in c.healthcheckInstanceHostMaps():
        if h.hostId == host.id:
            if hcihm.state == 'active':
                hcihm = h
                break

    assert hcim is None


def test_healtcheck(client, context, super_client):
    stack = client.create_environment(name='env-' + random_str())
    image_uuid = context.image_uuid
    register_simulated_host(context)

    # test that external service was set with healtcheck
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html",
                    "port": 200}
    launch_config = {"imageUuid": image_uuid, "healthCheck": health_check}
    service = client.create_service(name=random_str(),
                                    environmentId=stack.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    service = client.wait_success(service.activate(), 120)
    expose_map = find_one(service.serviceExposeMaps)
    c = super_client.reload(expose_map.instance())
    c_host_id = super_client.reload(c).instanceHostMaps()[0].hostId
    health_c = super_client. \
        list_healthcheckInstance(accountId=service.accountId, instanceId=c.id)
    assert len(health_c) > 0
    health_id = health_c[0].id

    def validate_container_host(host_maps):
        for host_map in host_maps:
            assert host_map.hostId != c_host_id

    host_maps = _wait_health_host_count(super_client, health_id, 3)
    validate_container_host(host_maps)

    # reactivate the service and
    # verify that its still has less than 3 healthchecks
    service = client.wait_success(service.deactivate(), 120)
    service = client.wait_success(service.activate(), 120)

    host_maps = _wait_health_host_count(super_client, health_id, 3)
    validate_container_host(host_maps)

    # reactivate the service, add 3 more hosts and verify
    # that healthcheckers number was completed to 3, excluding
    # container's host
    service = client.wait_success(service.deactivate(), 120)
    register_simulated_host(context)
    register_simulated_host(context)
    register_simulated_host(context)
    client.wait_success(service.activate(), 120)

    host_maps = _wait_health_host_count(super_client, health_id, 3)
    validate_container_host(host_maps)


def _wait_health_host_count(super_client, health_id, count):
    def active_len():
        match = super_client. \
            list_healthcheckInstanceHostMap(healthcheckInstanceId=health_id,
                                            state='active')
        if len(match) <= count:
            return match

    return wait_for(active_len)


def test_external_svc_healthcheck(client, context):
    env = client.create_environment(name='env-' + random_str())

    # test that external service was set with healtcheck
    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html",
                    "port": 200}
    ips = ["72.22.16.5", '192.168.0.10']
    service = client.create_externalService(name=random_str(),
                                            environmentId=env.id,
                                            externalIpAddresses=ips,
                                            healthCheck=health_check)
    service = client.wait_success(service)
    assert service.healthCheck.name == "check1"
    assert service.healthCheck.responseTimeout == 3
    assert service.healthCheck.interval == 4
    assert service.healthCheck.healthyThreshold == 5
    assert service.healthCheck.unhealthyThreshold == 6
    assert service.healthCheck.requestLine == "index.html"
    assert service.healthCheck.port == 200

    # test rancher-compose export
    compose_config = env.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.rancherComposeConfig)
    assert document[service.name]['health_check'] is not None


def _update_healthy(agent, hcihm, c, super_client):
    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy',
             timeout=5)
    return hcihm


def _update_unhealthy(agent, hcihm, c, super_client):
    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something bad',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'
    wait_for(lambda: super_client.reload(c).healthState == 'unhealthy',
             timeout=5)
