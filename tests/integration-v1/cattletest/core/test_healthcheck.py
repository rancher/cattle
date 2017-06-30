from common import *  # NOQA
from cattle import ApiError
import yaml


def _get_agent_for_container(context, container):
    agent = None
    for map in container.hosts()[0].instanceHostMaps():
        try:
            c = map.instance()
        except Exception:
            continue
        if c.agentId is not None:
            agent = c.agent()
            break

    if agent is None:
        client = context.client
        env = client.create_environment(name='env-' + random_str())
        svc = client.create_service(name='agentglobal' + random_str(),
                                    launchConfig={
            'imageUuid': context.image_uuid,
            "labels": {
                'io.rancher.scheduler.global': 'true',
                'io.rancher.container.create_agent': 'true'
            }
        }, stackId=env.id)
        svc = wait_state(client, svc, 'inactive')
        client.wait_success(svc.activate())

    for map in container.hosts()[0].instanceHostMaps():
        try:
            c = map.instance()
        except Exception:
            continue
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


def test_upgrade_with_health(client, context, super_client):
    env = client.create_environment(name='env-' + random_str())
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scale=1)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    env.activateservices()
    svc = client.wait_success(svc, 120)
    assert svc.state == "active"

    # upgrade the service and
    new_launch_config = {"imageUuid": image_uuid,
                         'healthCheck': {
                             'port': 80,
                         }}
    strategy = {"launchConfig": new_launch_config,
                "intervalMillis": 100}
    svc.upgrade_action(inServiceStrategy=strategy)

    def wait_for_map_count(service):
        m = super_client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == 2

    def wait_for_managed_map_count(service):
        m = super_client. \
            list_serviceExposeMap(serviceId=service.id, state='active',
                                  upgrade=False)
        return len(m) == 1

    wait_for_condition(client, svc, wait_for_map_count)
    wait_for_condition(client, svc, wait_for_managed_map_count)

    m = super_client. \
        list_serviceExposeMap(serviceId=svc.id, state='active', upgrade=False)

    c = super_client.reload(m[0].instance())

    wait_for(lambda: super_client.reload(c).state == 'running')

    c = super_client.reload(m[0].instance())

    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    # shouldn't become upgraded at this point
    try:
        wait_for(lambda: super_client.reload(svc).state == 'upgraded',
                 timeout=10)
    except Exception:
        pass

    _update_healthy(agent, hcihm, c, super_client)

    wait_for(lambda: super_client.reload(svc).healthState == 'healthy')
    wait_for(lambda: super_client.reload(svc).state == 'upgraded')
    super_client.reload(svc).remove()


def test_upgrade_start_first_with_health(client, context, super_client):
    env = client.create_environment(name='env-' + random_str())

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scale=1)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    env.activateservices()
    svc = client.wait_success(svc, 120)
    assert svc.state == "active"

    # upgrade the service and
    # check that c3 and c2 got the same ip
    new_launch_config = {"imageUuid": image_uuid,
                         'healthCheck': {
                             'port': 80,
                         }}
    strategy = {"launchConfig": new_launch_config,
                "intervalMillis": 100,
                "startFirst": True}
    svc.upgrade_action(inServiceStrategy=strategy)

    def wait_for_map_count(service):
        m = super_client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == 2

    def wait_for_managed_map_count(service):
        m = super_client. \
            list_serviceExposeMap(serviceId=service.id, state='active',
                                  upgrade=False)
        return len(m) == 1

    wait_for_condition(client, svc, wait_for_map_count)
    wait_for_condition(client, svc, wait_for_managed_map_count)

    m = super_client. \
        list_serviceExposeMap(serviceId=svc.id, state='active', upgrade=False)

    c = super_client.reload(m[0].instance())

    wait_for(lambda: super_client.reload(c).state == 'running')

    c = super_client.reload(m[0].instance())

    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    _update_healthy(agent, hcihm, c, super_client)

    wait_for(lambda: super_client.reload(svc).healthState == 'healthy')
    wait_for(lambda: super_client.reload(svc).state == 'upgraded')
    super_client.reload(svc).remove()


def test_health_check_create_instance(super_client, context):
    c = context.create_container(healthCheck={
        'port': 80,
    })

    assert c.healthCheck.port == 80

    c = super_client.reload(c)
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

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

    wait_for(lambda: super_client.reload(c).healthState == 'healthy')


def _create_svc_w_healthcheck(client, context):
    env = client.create_environment(name='env-' + random_str())
    return _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)


def test_health_check_create_service(super_client, context, client):
    service = _create_svc_w_healthcheck(client, context)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

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
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='INIT',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    # restart the instance
    c = super_client.wait_success(c.restart())
    wait_for(lambda: super_client.reload(c).state == 'running')
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something bad',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='INIT',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

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

    wait_for(lambda: super_client.reload(c).healthState == 'unhealthy')
    wait_for(lambda: len(service.serviceExposeMaps()) == 1)
    remove_service(service)


def test_health_check_ip_retain(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id, retainIp=True)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())
    ip1 = c1.primaryIpAddress
    hci = find_one(c1.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c1)

    assert hcihm.healthState == 'initializing'
    assert c1.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c1).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == c1.accountId
    assert se.instanceId == c1.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'
    assert hcihm.externalTimestamp == ts

    wait_for(lambda: super_client.reload(c1).healthState == 'unhealthy')

    def check():
        s = super_client.reload(service)
        return s.instanceIds is not None and len(s.instanceIds) == 1 and \
            c1.id not in s.instanceIds

    wait_for(check)
    for e_map in service.serviceExposeMaps():
        if e_map.instance().id == c1.id:
            continue
        c2 = wait_state(super_client, e_map.instance(), 'running')
        assert c2.name == c1.name
        assert c2.primaryIpAddress == ip1
        break
    remove_service(service)


def test_health_state_stack(super_client, context, client):
    c = client
    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)

    i = 'initializing'
    wait_for(lambda: super_client.reload(service).healthState == i)
    wait_for(lambda: client.reload(env).healthState == i)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())
    hci = find_one(c1.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c1)
    assert hcihm.healthState == 'initializing'
    assert c1.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c1).healthState == 'healthy')

    wait_for(lambda: super_client.reload(service).healthState == 'healthy')
    wait_for(lambda: c.reload(env).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm.uuid)

    se = super_client.wait_success(se)
    assert se.state == 'created'
    assert se.accountId == c1.accountId
    assert se.instanceId == c1.id
    assert se.healthcheckInstanceId == hci.id

    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'unhealthy'
    assert hcihm.externalTimestamp == ts

    wait_for(lambda: super_client.reload(c1).healthState == 'unhealthy')


def test_health_state_start_once(super_client, context, client):
    c = client
    env = client.create_environment(name='env-' + random_str())
    labels = {"io.rancher.container.start_once": "true"}
    svc = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        },
        'labels': labels
    }, stackId=env.id)

    wait_for(lambda: super_client.reload(svc).healthState == 'initializing')
    wait_for(lambda: c.reload(env).healthState == 'initializing')

    maps = _wait_until_active_map_count(svc, 1, client)
    expose_map = maps[0]
    c1 = super_client.reload(expose_map.instance())
    hci = find_one(c1.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c1)
    assert hcihm.healthState == 'initializing'
    assert c1.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c1).healthState == 'healthy')

    wait_for(lambda: super_client.reload(svc).healthState == 'healthy')
    wait_for(lambda: c.reload(env).healthState == 'healthy')

    super_client.wait_success(c1.stop())

    wait_for(lambda: super_client.reload(svc).healthState == 'started-once')
    wait_for(lambda: c.reload(env).healthState == 'started-once')
    remove_service(svc)


def test_health_state_sidekick_start_once(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    labels = {"io.rancher.container.start_once": "true"}
    slc = {'imageUuid': context.image_uuid, 'name': "test1"}
    svc = _create_and_activate_svc(client, 2, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'labels': labels
    }, stackId=env.id, secondaryLaunchConfigs=[slc])

    wait_for(lambda: super_client.reload(svc).healthState == 'healthy')
    wait_for(lambda: client.reload(env).healthState == 'healthy')
    remove_service(svc)


def test_health_state_selectors(context, client):
    env = client.create_environment(name='env-' + random_str())
    labels = {'foo': "bar"}
    container1 = client.create_container(imageUuid=context.image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container1 = client.wait_success(container1)
    assert container1.state == "running"

    launch_config = {"imageUuid": "rancher/none"}
    service = _create_and_activate_svc(client, 1, name=random_str(),
                                       stackId=env.id,
                                       launchConfig=launch_config,
                                       selectorContainer="foo=bar")
    service = client.wait_success(service)
    assert service.selectorContainer == "foo=bar"

    wait_for(lambda: client.reload(service).healthState == 'healthy')
    wait_for(lambda: client.reload(env).healthState == 'healthy')
    remove_service(service)


def test_svc_health_state(context, client):
    env = client.create_environment(name='env-' + random_str())

    launch_config = {"imageUuid": context.image_uuid}
    service = _create_and_activate_svc(client, 1, name=random_str(),
                                       stackId=env.id,
                                       launchConfig=launch_config)

    wait_for(lambda: client.reload(service).healthState == 'healthy')
    wait_for(lambda: client.reload(env).healthState == 'healthy')
    remove_service(service)


def test_health_check_init_timeout(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'initializingTimeout': 2000,
        }
    }, stackId=env.id)
    h_c = service.launchConfig.healthCheck
    assert h_c.initializingTimeout == 2000

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    # wait for the instance to be removed
    wait_for_condition(client, c, lambda x: x.removed is None)
    remove_service(service)


def test_health_check_reinit_timeout(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'reinitializingTimeout': 1,
        }
    }, stackId=env.id)
    h_c = service.launchConfig.healthCheck
    assert h_c.reinitializingTimeout == 1

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

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
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    # restart the instance
    c = super_client.wait_success(c.restart())
    wait_for(lambda: super_client.reload(c).state == 'running')
    wait_for(lambda: super_client.reload(c).healthState == 'reinitializing')

    # wait for the instance to be removed
    wait_for_condition(super_client, c,
                       lambda x: x.removed is not None)
    remove_service(service)


def test_health_check_bad_external_timestamp(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, container)
    agent_client = _get_agent_client(agent)

    assert hcihm.healthState == 'initializing'

    with pytest.raises(ApiError) as e:
        agent_client.create_service_event(reportedHealth='Something Bad',
                                          healthcheckUuid=hcihm.uuid)
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'externalTimestamp'
    remove_service(service)


def test_health_check_noop(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    svc = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
            'strategy': 'none'
        }
    }, stackId=env.id)

    assert svc.launchConfig.healthCheck.strategy == 'none'

    maps = _wait_until_active_map_count(svc, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    hci = find_one(c.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c)

    assert hcihm.healthState == 'initializing'
    assert c.healthState == 'initializing'

    hcihm = _update_healthy(agent, hcihm, c, super_client)
    svc = wait_state(client, svc, 'active')

    _update_unhealthy(agent, hcihm, c, super_client)

    svc = wait_state(client, svc, 'active')

    assert len(svc.serviceExposeMaps()) == 1
    c = super_client.wait_success(c)
    assert c.state == 'running'
    remove_service(svc)


def remove_service(svc):
    for i in range(1, 3):
        try:
            svc.remove()
            return
        except Exception:
            pass
        time.sleep(1)


def test_health_check_default(super_client, context, client):
    env = client.create_environment(name='env-' + random_str())
    svc = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80
        }
    }, stackId=env.id)

    expose_maps = svc.serviceExposeMaps()
    c1 = super_client.reload(expose_maps[0].instance())
    hci = find_one(c1.healthcheckInstances)
    hcihm = find_one(hci.healthcheckInstanceHostMaps)
    agent = _get_agent_for_container(context, c1)

    assert hcihm.healthState == 'initializing'
    assert c1.healthState == 'initializing'

    hcihm = _update_healthy(agent, hcihm, c1, super_client)

    # update unheatlhy, the container should be removed
    _update_unhealthy(agent, hcihm, c1, super_client)
    c1 = super_client.wait_success(c1)
    wait_for_condition(client, c1,
                       lambda x: x.removed is not None)
    wait_for(lambda: len(svc.serviceExposeMaps()) == 1)
    remove_service(svc)


def test_health_check_bad_agent(super_client, context, client):
    # Create another host to get the agent from that host
    host2 = super_client.reload(register_simulated_host(context))
    # register one more host to ensure
    # there is at least one more host
    # to schedule healtcheck on
    register_simulated_host(context)

    env = client.create_environment(name='env-' + random_str())
    s = _create_and_activate_svc(client, 'global', name='test', launchConfig={
        'imageUuid': context.image_uuid,
        'healthCheck': {
            'port': 80,
        },
        "labels": {
            'io.rancher.scheduler.global': 'true'
        }
    }, stackId=env.id)

    maps = _wait_until_active_map_count(s, 3, client)
    expose_map = maps[0]
    container = super_client.reload(expose_map.instance())
    hci = find_one(container.healthcheckInstances)
    hcihm = None
    wait_for(lambda: len(hci.healthcheckInstanceHostMaps()) > 1)
    for h in hci.healthcheckInstanceHostMaps():
        if h.hostId != host2.id:
            hcihm = h
            break
    assert hcihm is not None
    agent_client = _get_agent_client(host2.agent())

    assert hcihm.healthState == 'initializing'

    ts = int(time.time())
    with pytest.raises(ApiError) as e:
        agent_client.create_service_event(externalTimestamp=ts,
                                          reportedHealth='Something Bad',
                                          healthcheckUuid=hcihm.uuid)
    assert e.value.error.code == 'CantVerifyHealthcheck'
    remove_service(s)


def test_health_check_reconcile(super_client, new_context):
    super_client.reload(register_simulated_host(new_context))
    register_simulated_host(new_context)
    client = new_context.client

    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': new_context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    initial_len = len(c.healthcheckInstanceHostMaps())
    assert initial_len == 2

    for h in c.healthcheckInstanceHostMaps():
        assert h.healthState == c.healthState

    hcihm1 = c.healthcheckInstanceHostMaps()[0]
    hosts = super_client.list_host(uuid=hcihm1.host().uuid)
    assert len(hosts) == 1

    hcihm2 = c.healthcheckInstanceHostMaps()[1]
    hosts = super_client.list_host(uuid=hcihm2.host().uuid)
    assert len(hosts) == 1
    host2 = hosts[0]

    agent = _get_agent_for_container(new_context, c)

    assert hcihm1.healthState == 'initializing'
    assert hcihm2.healthState == 'initializing'
    assert c.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm1.uuid)
    super_client.wait_success(se)
    hcihm1 = super_client.wait_success(super_client.reload(hcihm1))
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm2.uuid)
    super_client.wait_success(se)
    super_client.wait_success(super_client.reload(hcihm2))
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='Something Bad',
                                     healthcheckUuid=hcihm1.uuid)
    super_client.wait_success(se)
    super_client.wait_success(super_client.reload(hcihm1))

    # still healthy as only one host reported healthy
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')
    # remove the host 1
    host2 = super_client.wait_success(host2.deactivate())
    host2 = super_client.wait_success(super_client.delete(host2))
    assert host2.state == 'removed'

    # should be unhealthy as the only health state reported is unhealthy
    wait_for(lambda: super_client.reload(c).healthState == 'unhealthy')
    remove_service(service)


def test_health_check_all_hosts_removed_reconcile(super_client, new_context):
    super_client.reload(register_simulated_host(new_context))
    client = new_context.client

    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': new_context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    wait_for(lambda: super_client.reload(c).state == 'running')
    initial_len = len(c.healthcheckInstanceHostMaps())
    assert initial_len == 1

    for h in c.healthcheckInstanceHostMaps():
        assert h.healthState == c.healthState

    hcihm1 = c.healthcheckInstanceHostMaps()[0]
    hosts = super_client.list_host(uuid=hcihm1.host().uuid)
    assert len(hosts) == 1
    host1 = hosts[0]

    agent = _get_agent_for_container(new_context, c)

    assert hcihm1.healthState == 'initializing'
    assert c.healthState == 'initializing'

    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm1.uuid)
    super_client.wait_success(se)
    super_client.wait_success(super_client.reload(hcihm1))
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')

    # remove health checker host
    host1 = super_client.wait_success(host1.deactivate())
    host1 = super_client.wait_success(super_client.delete(host1))
    assert host1.state == 'removed'

    # instance should stay as healthy
    try:
        wait_for(lambda: super_client.reload(c).healthState == 'unhealthy',
                 timeout=5)
    except Exception:
        pass

    wait_for(lambda: super_client.reload(c).healthState == 'healthy')
    remove_service(service)


def test_hosts_removed_reconcile_when_init(super_client, new_context):
    super_client.reload(register_simulated_host(new_context))
    client = new_context.client

    env = client.create_environment(name='env-' + random_str())
    service = _create_and_activate_svc(client, 1, name='test', launchConfig={
        'imageUuid': new_context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, stackId=env.id)

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
    c = super_client.reload(expose_map.instance())
    initial_len = len(c.healthcheckInstanceHostMaps())
    assert initial_len == 1

    for h in c.healthcheckInstanceHostMaps():
        assert h.healthState == c.healthState

    hcihm1 = c.healthcheckInstanceHostMaps()[0]
    hosts = super_client.list_host(uuid=hcihm1.host().uuid)
    assert len(hosts) == 1
    host1 = hosts[0]

    assert hcihm1.healthState == 'initializing'
    assert c.healthState == 'initializing'

    # remove the healthchecking host
    host1 = super_client.wait_success(host1.deactivate())
    host1 = super_client.wait_success(super_client.delete(host1))
    assert host1.state == 'removed'

    # instance should remain as healthy as there are no reporters at this point
    try:
        wait_for(lambda: super_client.reload(c).healthState == 'unhealthy',
                 timeout=5)
    except Exception:
        pass

    wait_for(lambda: super_client.reload(c).healthState == 'initializing')
    remove_service(service)


@pytest.mark.skipif('True')
def test_health_check_host_remove(super_client, new_context):
    client = new_context.client
    # create 4 hosts for healtcheck as one of them would be removed later
    super_client.reload(register_simulated_host(new_context))
    super_client.reload(register_simulated_host(new_context))
    super_client.reload(register_simulated_host(new_context))
    super_client.reload(register_simulated_host(new_context))

    env = client.create_environment(name='env-' + random_str())
    service = client.create_service(name='test', launchConfig={
        'imageUuid': new_context.image_uuid,
        'healthCheck': {
            'port': 80,
        }
    }, environmentId=env.id)

    service = client.wait_success(client.wait_success(service).activate())
    assert service.state == 'active'

    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
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
    wait_for(lambda: len(c.healthcheckInstanceHostMaps()) == initial_len)

    hcim = None
    for h in c.healthcheckInstanceHostMaps():
        if h.hostId == host.id:
            if hcihm.state == 'active':
                hcihm = h
                break

    assert hcim is None
    remove_service(service)


def test_healtcheck(new_context, super_client):
    client = new_context.client
    image_uuid = new_context.image_uuid
    stack = client.create_environment(name='env-' + random_str())
    host = register_simulated_host(new_context)
    client.wait_success(host)

    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html",
                    "port": 200}
    launch_config = {"imageUuid": image_uuid, "healthCheck": health_check}
    service = _create_and_activate_svc(client, 1, name=random_str(),
                                       stackId=stack.id,
                                       launchConfig=launch_config)
    maps = _wait_until_active_map_count(service, 1, client)
    expose_map = maps[0]
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

    # restart the instance
    # verify that its still has less than 3 healthchecks
    c = client.wait_success(c.stop())
    c = client.wait_success(c.start())

    host_maps = _wait_health_host_count(super_client, health_id, 3)
    validate_container_host(host_maps)

    # stop instance, add 3 more hosts, start instance and verify
    # that healthcheckers number was completed to 3, excluding
    # container's host
    c = client.wait_success(c.stop())
    for i in range(0, 3):
        host = register_simulated_host(new_context)
        client.wait_success(host)

    host_maps = _wait_health_host_count(super_client, health_id, 3)
    validate_container_host(host_maps)
    remove_service(service)


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
    document = yaml.load(compose_config.dockerComposeConfig)
    assert document['services'][service.name]['health_check'] is not None


def _update_healthy(agent, hcihm, c, super_client):
    ts = int(time.time())
    client = _get_agent_client(agent)
    se = client.create_service_event(externalTimestamp=ts,
                                     reportedHealth='UP',
                                     healthcheckUuid=hcihm.uuid)
    super_client.wait_success(se)
    hcihm = super_client.wait_success(super_client.reload(hcihm))
    assert hcihm.healthState == 'healthy'
    wait_for(lambda: super_client.reload(c).healthState == 'healthy')
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
    wait_for(lambda: super_client.reload(c).healthState == 'unhealthy')


def _wait_until_active_map_count(service, count, client):
    def wait_for_map_count(service):
        m = client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == count

    wait_for_condition(client, service, wait_for_map_count)
    return client. \
        list_serviceExposeMap(serviceId=service.id, state='active')


def test_global_service_health(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    client.wait_success(host1)
    client.wait_success(host2)

    # create environment and services
    env = client.create_environment(name='env-' + random_str())
    image_uuid = new_context.image_uuid
    labels = {"io.rancher.container.start_once": "true"}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary",
                    "labels": labels}
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true'
        }
    }
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary_lc])
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc = client.wait_success(svc.activate(), 120)

    wait_for(lambda: client.reload(svc).healthState == 'healthy')

    # stop instances
    c1 = _validate_compose_instance_start(client, svc, env, "1",
                                          "secondary")
    c2 = _validate_compose_instance_start(client, svc, env, "2",
                                          "secondary")
    client.wait_success(c1.stop())
    client.wait_success(c2.stop())

    wait_for(lambda: client.reload(svc).healthState == 'healthy')


def _validate_compose_instance_start(client, service, env,
                                     number, launch_config_name=None):
    cn = launch_config_name + "-" if \
        launch_config_name is not None else ""
    name = env.name + "-" + service.name + "-" + cn + number

    def wait_for_map_count(service):
        instances = client. \
            list_container(name=name,
                           state="running")
        return len(instances) == 1

    wait_for(lambda: wait_for_condition(client, service,
                                        wait_for_map_count), timeout=5)

    instances = client. \
        list_container(name=name,
                       state="running")
    return instances[0]


def test_stack_health_state(super_client, context, client):
    c = client
    env = client.create_environment(name='env-' + random_str())
    svc = client.create_service(name='test', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)
    svc = client.wait_success(client.wait_success(svc).activate())
    assert svc.state == 'active'

    wait_for(lambda: super_client.reload(svc).healthState == 'healthy')
    wait_for(lambda: c.reload(env).healthState == 'healthy')
    remove_service(svc)
    wait_for(lambda: super_client.reload(svc).state == 'removed')
    time.sleep(3)
    wait_for(lambda: c.reload(env).healthState == 'healthy')


def _create_and_activate_svc(client, count, **kw):
    svc = client.create_service(kw)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc.activate()
    # wait for service instances to become running
    if count == 'global':
        time.sleep(2)
        wait_for(lambda: len(client.reload(svc).instances()) >= 1)
    else:
        wait_for(lambda: len(client.reload(svc).instances()) == count)
    for i in client.reload(svc).instances():
        wait_for(lambda: client.reload(i).state == 'running')
    return client.reload(svc)
