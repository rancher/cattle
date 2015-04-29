from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return nsp


def random_str():
    return 'random{0}'.format(random_num())


def test_create_env_and_svc(client, sim_context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    # create service
    service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # verify that the load balancer was created for the service
    lb = client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lb) == 1
    assert lb[0].state == 'active'


def test_activate_lb_svc(super_client, admin_client, sim_context, nsp):
    host = sim_context['host']
    user_account_id = host.accountId
    env = admin_client.create_environment(name=random_str(),
                                          accountId=user_account_id)
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}

    service = super_client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   networkId=nsp.networkId,
                                   launchConfig=launch_config,
                                   accountId=user_account_id,
                                   loadBalancerInstanceUriPredicate='sim://')
    service = super_client.wait_success(service)
    assert service.state == "inactive"
    service = wait_success(admin_client, service.activate(), 120)
    # perform validation
    lb, service = _validate_lb_service_activate(env, host,
                                                service, super_client)
    _validate_lb_instance(host, lb, super_client, service)


def _activate_svc_w_scale_two(admin_client, random_str, super_client):
    cred = create_user(admin_client,
                       random_str,
                       kind='user')
    account = cred[2]
    user_account_id = account.id
    sim_context_local1 = create_sim_context(super_client,
                                            "local1" + random_str,
                                            ip='192.168.11.6',
                                            account=account)
    sim_context_local2 = create_sim_context(super_client,
                                            "local2" + random_str,
                                            ip='192.168.11.6',
                                            account=account)
    host1 = sim_context_local1["host"]
    host2 = sim_context_local2["host"]
    nsp = create_agent_instance_nsp(super_client, sim_context_local1)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)
    env = admin_client.create_environment(name=random_str,
                                          accountId=user_account_id)
    env = admin_client.wait_success(env)
    assert env.state == "active"
    image_uuid = sim_context_local1['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}
    service = super_client. \
        create_loadBalancerService(name=random_str,
                                   environmentId=env.id,
                                   networkId=nsp.networkId,
                                   launchConfig=launch_config,
                                   accountId=user_account_id,
                                   loadBalancerInstanceUriPredicate='sim://',
                                   scale=2)
    service = super_client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = super_client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = super_client.wait_success(lbs[0])
    assert lb.state == 'active'
    return host1, host2, lb, service, env


def test_deactivate_then_remove_lb_svc(super_client, admin_client):
    host1, host2, lb, service, env = _activate_svc_w_scale_two(admin_client,
                                                               random_str(),
                                                               super_client)

    # 1. verify that all hosts mappings are created
    validate_add_host(host1, lb, super_client)
    validate_add_host(host2, lb, super_client)

    # 2. deactivate service and validate that
    # the hosts mappings are gone,
    # but lb still present
    service = wait_success(admin_client, service.deactivate())
    validate_remove_host(host1, lb, super_client)
    validate_remove_host(host2, lb, super_client)

    lb = super_client.reload(lb)
    assert lb.state == "active"

    # remove service and verify that the lb is gone
    wait_success(admin_client, service.remove())
    wait_for_condition(super_client, lb, _resource_is_removed,
                       lambda x: 'State is: ' + x.state)


def test_remove_active_lb_svc(super_client, admin_client):
    host1, host2, lb, service, env = _activate_svc_w_scale_two(admin_client,
                                                               random_str(),
                                                               super_client)

    # 1. verify that all hosts mappings are created/updated
    validate_add_host(host1, lb, super_client)
    validate_add_host(host2, lb, super_client)

    # 2. delete service and validate that the hosts mappings are gone,
    # and lb is gone as well as lb config/listeners
    wait_success(admin_client, service.remove())
    validate_remove_host(host1, lb, super_client)
    validate_remove_host(host2, lb, super_client)

    wait_for_condition(
        super_client, lb, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    lb_configs = super_client. \
        list_loadBalancerConfig(name=env.name + "_" + service.name)

    assert len(lb_configs) == 1
    lb_config = lb_configs[0]
    lb_config = super_client.wait_success(lb_config)
    assert lb_config.state == "removed"


def test_targets(super_client, admin_client, sim_context, nsp):
    host = sim_context['host']
    user_account_id = host.accountId
    env = admin_client.create_environment(name=random_str(),
                                          accountId=user_account_id)
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create web, db lb services
    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}
    web_service = super_client. \
        create_service(name=random_str() + "web",
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config)

    web_service = super_client.wait_success(web_service)

    db_service = super_client. \
        create_service(name=random_str() + "db",
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config)

    db_service = super_client.wait_success(db_service)

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [8081, '909:1001']}
    lb_service = super_client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   networkId=nsp.networkId,
                                   launchConfig=lb_launch_config,
                                   accountId=user_account_id,
                                   loadBalancerInstanceUriPredicate='sim://')
    lb_service = super_client.wait_success(lb_service)
    assert lb_service.state == "inactive"

    # map web service to lb service - early binding,
    # before services are activated
    lb_service = lb_service.addservicelink(serviceId=web_service.id)

    # activate web and lb services
    lb_service = wait_success(admin_client, lb_service.activate(), 120)
    _validate_lb_service_activate(env, host, lb_service, super_client)
    web_service = wait_success(admin_client, web_service.activate(), 120)
    assert web_service.state == "active"
    db_service = wait_success(admin_client, db_service.activate(), 120)
    assert db_service.state == "active"

    # bind db and lb services after service is activated
    lb_service.addservicelink(serviceId=db_service.id)

    # verify that instances of db and web services were added to lb
    web_instances = super_client. \
        list_container(name=env.name + "_" + web_service.name + "_" + "1")
    assert len(web_instances) == 1
    _validate_add_target(web_instances[0], super_client)

    db_instances = super_client. \
        list_container(name=env.name + "_" + db_service.name + "_" + "1")
    assert len(db_instances) == 1
    _validate_add_target(db_instances[0], super_client)

    # remove link and make sure that the target map is gone
    lb_service.removeservicelink(serviceId=db_service.id)
    # validate that the instance is still running
    db_instance = super_client.reload(db_instances[0])
    assert db_instance.state == 'running'

    _validate_remove_target(db_instance, super_client)


def test_create_svc_with_lb_config(sim_context, client):
    name = random_str()
    env = client.create_environment(name=name)
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    health_check = {"responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html"}

    app_policy = {"name": "policy1", "cookie": "cookie1",
                  "maxLength": 4, "prefix": "true",
                  "requestLearn": "false", "timeout": 10,
                  "mode": "query_string"}

    lb_policy = {"name": "policy2", "cookie": "cookie1",
                 "domain": ".test.com", "indirect": "true",
                 "nocache": "true", "postonly": "true",
                 "mode": "insert"}

    lb_config = {"name": name, "healthCheck": health_check,
                 "appCookieStickinessPolicy": app_policy,
                 "lbCookieStickinessPolicy": lb_policy}

    # create service
    service = client. \
        create_loadBalancerService(name=name,
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   loadBalancerConfig=lb_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    assert service.loadBalancerConfig is not None

    # verify that the load balancer was created for the service
    lb = client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lb) == 1
    assert lb[0].state == 'active'

    # verify the load balancer config info
    configs = client. \
        list_loadBalancerConfig(name=name)

    assert len(configs) == 1
    config = configs[0]
    assert config.healthCheck is not None
    assert config.healthCheck.responseTimeout == 3
    assert config.healthCheck.interval == 4
    assert config.healthCheck.healthyThreshold == 5
    assert config.healthCheck.unhealthyThreshold == 6
    assert config.healthCheck.requestLine == "index.html"

    assert config.appCookieStickinessPolicy is not None
    assert config.appCookieStickinessPolicy.name == "policy1"
    assert config.appCookieStickinessPolicy.cookie == "cookie1"
    assert config.appCookieStickinessPolicy.maxLength == 4
    assert config.appCookieStickinessPolicy.prefix is True
    assert config.appCookieStickinessPolicy.requestLearn is False
    assert config.appCookieStickinessPolicy.timeout == 10
    assert config.appCookieStickinessPolicy.mode == "query_string"

    assert config.lbCookieStickinessPolicy is not None
    assert config.lbCookieStickinessPolicy.name == "policy2"
    assert config.lbCookieStickinessPolicy.cookie == "cookie1"
    assert config.lbCookieStickinessPolicy.domain == ".test.com"
    assert config.lbCookieStickinessPolicy.indirect is True
    assert config.lbCookieStickinessPolicy.nocache is True
    assert config.lbCookieStickinessPolicy.postonly is True
    assert config.lbCookieStickinessPolicy.mode == "insert"


def test_scale(admin_client, super_client):
    cred = create_user(admin_client,
                       random_str(),
                       kind='user')
    account = cred[2]
    user_account_id = account.id
    sim_context_local1 = create_sim_context(super_client,
                                            "local1" + random_str(),
                                            ip='192.168.11.6',
                                            account=account)
    sim_context_local2 = create_sim_context(super_client,
                                            "local2" + random_str(),
                                            ip='192.168.11.6',
                                            account=account)
    host1 = sim_context_local1["host"]
    host2 = sim_context_local2["host"]
    nsp = create_agent_instance_nsp(super_client, sim_context_local1)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)
    env = admin_client.create_environment(name=random_str(),
                                          accountId=user_account_id)
    env = admin_client.wait_success(env)
    assert env.state == "active"
    image_uuid = sim_context_local1['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}
    service = super_client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   networkId=nsp.networkId,
                                   launchConfig=launch_config,
                                   accountId=user_account_id,
                                   loadBalancerInstanceUriPredicate='sim://')
    service = super_client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = super_client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = super_client.wait_success(lbs[0])
    assert lb.state == 'active'

    # validate that one host map was created
    _wait_until_active_map_count(lb, 1, super_client)

    # scale up
    service = admin_client.update(service, scale=2)
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 2
    _wait_until_active_map_count(lb, 2, super_client)

    # now scale down
    service = admin_client.update(service, scale=0)
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 0
    _wait_until_active_map_count(lb, 0, super_client)
    validate_remove_host(host1, lb, super_client)
    validate_remove_host(host2, lb, super_client)


def _wait_until_active_map_count(lb, count, super_client, timeout=30):
    start = time.time()
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 state="active")
    while len(host_maps) != count:
        time.sleep(.5)
        host_maps = super_client.\
            list_loadBalancerHostMap(loadBalancerId=lb.id, state="active")
        if time.time() - start > timeout:
            assert 'Timeout waiting for agent to be removed.'

    return


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def validate_add_host(host, lb, super_client):
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host.id


def validate_remove_host(host, lb, super_client):
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host.id


def _validate_lb_instance(host, lb, super_client, service):
    # verify that the agent got created
    uri = 'sim://?lbId={}&hostId={}'. \
        format(get_plain_id(super_client, lb),
               get_plain_id(super_client, host))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1
    # verify that the agent instance got created
    agent_instances = super_client.list_instance(agentId=agents[0].id)
    assert len(agent_instances) == 1

    # verify that the instance was registered within the service
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id,
                              instanceId=agent_instances[0].id)
    assert len(instance_service_map) == 1


def _validate_lb_service_activate(env, host, service, super_client):
    # 1. verify that the service was activated
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = super_client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = super_client.wait_success(lbs[0])
    assert lb.state == 'active'
    # 3. verify host mapping got created
    validate_add_host(host, lb, super_client)
    # 4. verify that listeners are created and mapped to the config
    config_id = lb.loadBalancerConfigId
    l_name = env.name + "_" + service.name + "_" + "8081"
    listeners = super_client. \
        list_loadBalancerListener(sourcePort=8081,
                                  name=l_name)
    assert len(listeners) >= 1
    listener = listeners[0]
    assert listener.sourcePort == 8081
    assert listener.targetPort == 8081

    validate_add_listener(config_id, listener, super_client)

    l_name = env.name + "_" + service.name + "_" + "909"
    listeners = super_client. \
        list_loadBalancerListener(sourcePort=909, name=l_name)
    assert len(listeners) >= 1
    listener = listeners[0]
    assert listener.sourcePort == 909
    assert listener.targetPort == 1001

    validate_add_listener(config_id, listener, super_client)
    return lb, service


def validate_add_listener(config_id, listener, super_client):
    lb_config_maps = _wait_until_map_created(config_id, listener, super_client)
    config_map = lb_config_maps[0]
    wait_for_condition(
        super_client, config_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _wait_until_map_created(config_id, listener, super_client, timeout=30):
    start = time.time()
    l_id = listener.id
    lb_config_maps = super_client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=l_id,
                                           loadBalancerConfigId=config_id)
    while len(lb_config_maps) == 0:
        time.sleep(.5)
        lb_config_maps = super_client. \
            list_loadBalancerConfigListenerMap(loadBalancerListenerId=l_id,
                                               loadBalancerConfigId=config_id)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map creation'
    return lb_config_maps


def _wait_until_target_map_created(super_client, container, timeout=30):
    start = time.time()
    target_maps = super_client. \
        list_loadBalancerTarget(instanceId=container.id)
    while len(target_maps) == 0:
        time.sleep(.5)
        target_maps = super_client. \
            list_loadBalancerTarget(instanceId=container.id)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map creation'
    return target_maps


def _validate_add_target(container, super_client):
    target_maps = _wait_until_target_map_created(super_client, container)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_target(container, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(instanceId=container.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)
