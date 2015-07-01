from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def image_uuid(context):
    return context.image_uuid


def test_create_env_and_svc(client, image_uuid):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

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


def test_activate_lb_svc(super_client, context, client, image_uuid):
    host = context.host
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid,
                     "ports": [8082, '910:1001']}

    service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    # perform validation
    lb, service = _validate_lb_service_activate(env, host,
                                                service, client,
                                                ['8082:8082', '910:1001'])
    _validate_lb_instance(host, lb, super_client, service)


def _activate_svc_w_scale_two(new_context, random_str):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    env = client.create_environment(name=random_str)
    env = client.wait_success(env)
    assert env.state == "active"
    launch_config = {"imageUuid": new_context.image_uuid,
                     "ports": [8081, '909:1001']}
    service = client. \
        create_loadBalancerService(name=random_str,
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   scale=2)
    service = client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = client.list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = client.wait_success(lbs[0])
    assert lb.state == 'active'
    return host1, host2, lb, service, env


def test_deactivate_then_activate_lb_svc(super_client, new_context):
    client = new_context.client
    host1, host2, lb, service, env = _activate_svc_w_scale_two(new_context,
                                                               random_str())

    # 1. verify that all hosts mappings are created
    validate_add_host(host1, lb, client)
    validate_add_host(host2, lb, client)

    # 2. deactivate service and validate that
    # the hosts mappings are still around,
    # and lb still present
    service = client.wait_success(service.deactivate())
    validate_add_host(host1, lb, client)
    validate_add_host(host2, lb, client)

    lb = super_client.reload(lb)
    assert lb.state == "inactive"

    # 3. activate service again
    service = client.wait_success(service.activate())
    assert service.state == 'active'
    lb = super_client.reload(lb)
    assert lb.state == "active"
    _validate_lb_instance(host1, lb, super_client, service)
    _validate_lb_instance(host2, lb, super_client, service)


def test_deactivate_then_remove_lb_svc(new_context):
    client = new_context.client
    host1, host2, lb, service, env = _activate_svc_w_scale_two(new_context,
                                                               random_str())
    # 1. verify that all hosts mappings are created
    validate_add_host(host1, lb, client)
    validate_add_host(host2, lb, client)

    # 2. deactivate service and validate that
    # the hosts mappings are still around,
    # and lb still present
    service = client.wait_success(service.deactivate())
    validate_add_host(host1, lb, client)
    validate_add_host(host2, lb, client)

    lb = client.reload(lb)
    assert lb.state == "inactive"

    # try to remove lb - should fail
    with pytest.raises(ApiError) as e:
        lb = client.wait_success(client.delete(lb))
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidAction'
    assert e.value.error.fieldName == 'serviceId'

    # remove service and verify that the lb is gone
    client.wait_success(service.remove())
    wait_for_condition(client, lb, _resource_is_removed,
                       lambda x: 'State is: ' + x.state)


def test_remove_active_lb_svc(new_context):
    client = new_context.client
    host1, host2, lb, service, env = _activate_svc_w_scale_two(new_context,
                                                               random_str())

    # 1. verify that all hosts mappings are created/updated
    validate_add_host(host1, lb, client)
    validate_add_host(host2, lb, client)

    # 2. delete service and validate that the hosts mappings are gone,
    # and lb is gone as well as lb config/listeners
    client.wait_success(service.remove())
    validate_remove_host(host1, lb, client)
    validate_remove_host(host2, lb, client)

    wait_for_condition(
        client, lb, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    lb_configs = client. \
        list_loadBalancerConfig(name=env.name + "_" + service.name)

    assert len(lb_configs) == 1
    lb_config = lb_configs[0]
    lb_config = client.wait_success(lb_config)
    assert lb_config.state == "removed"


def test_targets(client, context):
    host = context.host
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create web, db lb services
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    web_service = client. \
        create_service(name=random_str() + "web",
                       environmentId=env.id,
                       launchConfig=launch_config)

    web_service = client.wait_success(web_service)

    db_service = client. \
        create_service(name=random_str() + "db",
                       environmentId=env.id,
                       launchConfig=launch_config)

    db_service = client.wait_success(db_service)

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [8081, '909:1001']}
    lb_service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config)
    lb_service = client.wait_success(lb_service)
    assert lb_service.state == "inactive"

    # map web service to lb service - early binding,
    # before services are activated
    service_link = {"serviceId": web_service.id}
    lb_service = lb_service.addservicelink(serviceLink=service_link)

    # activate web and lb services
    lb_service = client.wait_success(lb_service.activate(), 120)
    _validate_lb_service_activate(env, host, lb_service, client,
                                  ['8081:8081', '909:1001'])
    web_service = client.wait_success(web_service.activate(), 120)
    assert web_service.state == "active"
    db_service = client.wait_success(db_service.activate(), 120)
    assert db_service.state == "active"

    # bind db and lb services after service is activated
    service_link = {"serviceId": db_service.id}
    lb_service.addservicelink(serviceLink=service_link)

    # verify that instances of db and web services were added to lb
    web_instances = client. \
        list_container(name=env.name + "_" + web_service.name + "_" + "1")
    assert len(web_instances) == 1
    _validate_add_target_instance(web_instances[0], client)

    db_instances = client. \
        list_container(name=env.name + "_" + db_service.name + "_" + "1")
    assert len(db_instances) == 1
    _validate_add_target_instance(db_instances[0], client)

    # remove link and make sure that the target map is gone
    service_link = {"serviceId": db_service.id}
    lb_service.removeservicelink(serviceLink=service_link)
    # validate that the instance is still running
    db_instance = client.reload(db_instances[0])
    assert db_instance.state == 'running'

    _validate_remove_target_instance(db_instance, client)


def test_target_ips(client, context):
    host = context.host
    user_account_id = host.accountId
    env = client.create_environment(name=random_str(),
                                    accountId=user_account_id)
    env = client.wait_success(env)
    assert env.state == "active"

    # create web, db lb services
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    web_ips = ["72.22.16.5", '72.22.16.6']
    web_service = client. \
        create_externalService(name=random_str() + "web",
                               environmentId=env.id,
                               launchConfig=launch_config,
                               externalIpAddresses=web_ips)

    web_service = client.wait_success(web_service)

    db_ips = ["192.168.0.9", '192.168.0.10']
    db_service = client. \
        create_externalService(name=random_str() + "db",
                               environmentId=env.id,
                               launchConfig=launch_config,
                               externalIpAddresses=db_ips)

    db_service = client.wait_success(db_service)

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [1010, '111:111']}
    lb_service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config,
                                   accountId=user_account_id,
                                   loadBalancerInstanceUriPredicate='sim://')
    lb_service = client.wait_success(lb_service)
    assert lb_service.state == "inactive"

    # map web service to lb service - early binding,
    # before services are activated
    service_link = {"serviceId": web_service.id}
    lb_service = lb_service.addservicelink(serviceLink=service_link)

    # activate web and lb services
    lb_service = client.wait_success(lb_service.activate(), 120)
    _validate_lb_service_activate(env, host, lb_service, client,
                                  ['1010:1010', '111:111'])
    web_service = client.wait_success(web_service.activate(), 120)
    assert web_service.state == "active"
    db_service = client.wait_success(db_service.activate(), 120)
    assert db_service.state == "active"

    # bind db and lb services after service is activated
    service_link = {"serviceId": db_service.id}
    lb_service.addservicelink(serviceLink=service_link)

    # verify that ips of db and web services were added to lb
    _validate_add_target_ip("72.22.16.5", client)
    _validate_add_target_ip("72.22.16.6", client)
    _validate_add_target_ip("192.168.0.9", client)
    _validate_add_target_ip("192.168.0.10", client)

    # remove link and make sure that the db targets are gone
    service_link = {"serviceId": db_service.id}
    lb_service.removeservicelink(serviceLink=service_link)
    _validate_remove_target_ip("192.168.0.9", client)
    _validate_remove_target_ip("192.168.0.10", client)

    # remove web service and validate that the web targets are gone
    client.wait_success(web_service.remove())
    _validate_remove_target_ip("72.22.16.5", client)
    _validate_remove_target_ip("72.22.16.6", client)


def test_create_svc_with_lb_config(context, client):
    name = random_str()
    env = client.create_environment(name=name)
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
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


def test_scale(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}
    service = client.create_loadBalancerService(name=random_str(),
                                                environmentId=env.id,
                                                launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = client.wait_success(lbs[0])
    assert lb.state == 'active'

    # validate that one host map was created
    _wait_until_active_map_count(lb, 1, client)

    # scale up
    service = client.update(service, scale=2, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 2
    _wait_until_active_map_count(lb, 2, client)

    # now scale down
    service = client.update(service, scale=0, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 0
    _wait_until_active_map_count(lb, 0, client)
    validate_remove_host(host1, lb, client)
    validate_remove_host(host2, lb, client)


def test_labels(super_client, client, context):
    host = context.host
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service with labels, and validate all of them
    # plus service label were set
    service_name = random_str()
    initial_labels = {'affinity': "container==B", '!affinity': "container==C"}
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8010, '913:913'], "labels": initial_labels}

    service = client. \
        create_loadBalancerService(name=service_name,
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    lb, service = _validate_lb_service_activate(env, host,
                                                service, client,
                                                ['8010:8010', '913:913'])
    lb_instance = _validate_lb_instance(host, lb, super_client, service)
    result_labels = {'affinity': "container==B", '!affinity': "container==C",
                     'io.rancher.project_service.name':
                         env.name + "/" + service_name}

    assert all(item in lb_instance.labels.items()
               for item in result_labels.items()) is True

    # create service w/o labels, and validate that
    # only one service label was set
    service_name = random_str()
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8089, '914:914']}

    service = client. \
        create_loadBalancerService(name=service_name,
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    lb, service = _validate_lb_service_activate(env, host,
                                                service, client,
                                                ['8089:8089', '914:914'])
    lb_instance = _validate_lb_instance(host, lb, super_client, service)
    result_labels = {'io.rancher.project_service.name':
                     env.name + '/' + service_name}
    assert all(item in lb_instance.labels.items()
               for item in result_labels.items()) is True


def test_inactive_lb(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create and activate web service
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    web_service = client. \
        create_service(name=random_str() + "web",
                       environmentId=env.id,
                       launchConfig=launch_config)

    web_service = client.wait_success(web_service)
    web_service = client.wait_success(web_service.activate(), 120)
    assert web_service.state == "active"
    web_instances = client. \
        list_container(name=env.name + "_" + web_service.name + "_" + "1")
    assert len(web_instances) == 1

    # create lb service, but don't activate
    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [1000]}
    lb_service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config)
    lb_service = client.wait_success(lb_service)
    assert lb_service.state == "inactive"
    lbs = client. \
        list_loadBalancer(serviceId=lb_service.id)
    assert len(lbs) == 1
    lb = lbs[0]

    # map web service to lb service; validate no lb targets were created
    service_link = {"serviceId": web_service.id}
    lb_service = lb_service.addservicelink(serviceLink=service_link)
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id)
    assert len(target_maps) == 0

    # activate lb service and validate web instance was added as lb target
    lb_service = client.wait_success(lb_service.activate(), 120)
    assert lb_service.state == "active"
    _validate_add_target_instance(web_instances[0], client)

    # deactivate lb service, and remove service link
    lb_service = client.wait_success(lb_service.deactivate(), 120)
    assert lb_service.state == "inactive"
    service_link = {"serviceId": web_service.id}
    lb_service = lb_service.removeservicelink(serviceLink=service_link)
    lb_service = client.wait_success(lb_service.activate(), 120)
    assert lb_service.state == "active"
    _validate_remove_target_instance(web_instances[0], client)


def test_destroy_svc_instance(super_client, context, client, image_uuid):
    host = context.host
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid,
                     "ports": [95, '94:94']}

    service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    # perform validation
    lb, service = _validate_lb_service_activate(env, host,
                                                service, client,
                                                ['94:94', '95:95'])
    instance = _validate_lb_instance(host, lb, super_client, service)
    client.wait_success(client.delete(instance))
    _wait_until_active_map_count(lb, 0, client)

    client.wait_success(service)
    _wait_until_active_map_count(lb, 1, client)
    _validate_lb_instance(host, lb, super_client, service)


def _wait_until_active_map_count(lb, count, super_client, timeout=30):
    start = time.time()
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 state="active")
    while len(host_maps) != count:
        time.sleep(.5)
        host_maps = super_client. \
            list_loadBalancerHostMap(loadBalancerId=lb.id, state="active")
        if time.time() - start > timeout:
            assert 'Timeout waiting for agent to be removed.'

    return


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def validate_add_host(host, lb, client):
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        client, host_map, _resource_is_active,
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
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id, state='active')
    assert len(host_maps) == 1
    # verify that the agent got created
    uri = 'delegate:///?lbId={}&hostMapId={}'. \
        format(get_plain_id(super_client, lb),
               get_plain_id(super_client, host_maps[0]))
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
    return agent_instances[0]


def _validate_create_listener(env, service, source_port,
                              client, target_port):
    l_name = env.name + "_" + service.name + "_" + source_port
    listeners = client. \
        list_loadBalancerListener(sourcePort=source_port,
                                  name=l_name)
    assert len(listeners) >= 1
    listener = listeners[0]
    assert listener.sourcePort == int(source_port)
    assert listener.targetPort == int(target_port)
    return listener


def _validate_lb_service_activate(env, host, service, client, ports):
    # 1. verify that the service was activated
    assert service.state == "active"
    # 2. verify that lb got created
    lbs = client. \
        list_loadBalancer(serviceId=service.id)
    assert len(lbs) == 1
    lb = client.wait_success(lbs[0])
    assert lb.state == 'active'
    # 3. verify host mapping got created
    validate_add_host(host, lb, client)
    # 4. verify that listeners are created and mapped to the config
    config_id = lb.loadBalancerConfigId
    source_port = ports[0].split(':')[0]
    target_port = ports[0].split(':')[1]
    listener = _validate_create_listener(env, service, source_port,
                                         client, target_port)
    _validate_add_listener(config_id, listener, client)

    source_port = ports[1].split(':')[0]
    target_port = ports[1].split(':')[1]
    listener = _validate_create_listener(env, service, source_port,
                                         client, target_port)
    _validate_add_listener(config_id, listener, client)
    return lb, service


def _validate_add_listener(config_id, listener, client):
    lb_config_maps = _wait_until_map_created(config_id, listener, client)
    config_map = lb_config_maps[0]
    wait_for_condition(
        client, config_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _wait_until_map_created(config_id, listener, client, timeout=30):
    start = time.time()
    l_id = listener.id
    lb_config_maps = client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=l_id,
                                           loadBalancerConfigId=config_id)
    while len(lb_config_maps) == 0:
        time.sleep(.5)
        lb_config_maps = client. \
            list_loadBalancerConfigListenerMap(loadBalancerListenerId=l_id,
                                               loadBalancerConfigId=config_id)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map creation'
    return lb_config_maps


def _wait_until_target_instance_map_created(super_client,
                                            container, timeout=30):
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


def _validate_add_target_ip(ip, super_client):
    target_maps = _wait_until_target_ip_map_created(super_client, ip)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_target_instance(container, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(instanceId=container.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _validate_remove_target_ip(ip, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(ipAddress=ip)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _validate_add_target_instance(container, super_client):
    target_maps = _wait_until_target_instance_map_created(super_client,
                                                          container)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _wait_until_target_ip_map_created(super_client, ip, timeout=30):
    start = time.time()
    target_maps = super_client. \
        list_loadBalancerTarget(ipAddress=ip)
    while len(target_maps) == 0:
        time.sleep(.5)
        target_maps = super_client. \
            list_loadBalancerTarget(ipAddress=ip)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map creation'
    return target_maps
