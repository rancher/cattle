from common_fixtures import *  # NOQA
from cattle import ApiError
import yaml


RESOURCE_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            'resources/certs')


@pytest.fixture(scope='module')
def image_uuid(context):
    return context.image_uuid


def test_create_env_and_svc(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    # create service
    service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    assert service.launchConfig.healthCheck is not None


def test_activate_lb_svc(super_client, context, client, image_uuid):
    context.host
    env = _create_stack(client)
    ports = ['8189:8189', '910:1001']
    launch_config = {"imageUuid": image_uuid,
                     "ports": ports}

    svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    assert svc.launchConfig.ports == ports

    svc = client.wait_success(svc.activate(), 120)
    # perform validation
    svc = _validate_lb_svc_activate(env, svc, client,
                                    ['8082:8082', '910:1001'])
    _validate_svc_instance_map_count(client, svc, "active", 1)


def test_deactivate_then_activate_lb_svc(super_client, new_context):
    client = new_context.client
    host1, host2, service, env = _activate_svc_w_scale_two(new_context,
                                                           random_str())

    # 1. verify that all hosts mappings are created
    _validate_svc_instance_map_count(client, service, "active", 2)

    # 2. deactivate service and validate that
    # the instance mappings are still around
    service = client.wait_success(service.deactivate())
    _validate_svc_instance_map_count(client, service, "active", 2)

    # 3. activate service again
    service = client.wait_success(service.activate())
    assert service.state == 'active'
    _validate_svc_instance_map_count(client, service, "active", 2)


def test_deactivate_then_remove_lb_svc(new_context):
    client = new_context.client
    host1, host2, service, env = _activate_svc_w_scale_two(new_context,
                                                           random_str())
    # 1. verify that all instances are created
    _validate_svc_instance_map_count(client, service, "active", 2)

    # 2. deactivate service and validate that
    # instances mappings are still around
    # and lb still present
    service = client.wait_success(service.deactivate())
    _validate_svc_instance_map_count(client, service, "active", 2)

    # remove service and verify that the lb is gone
    client.wait_success(service.remove())


def test_remove_active_lb_svc(new_context):
    client = new_context.client
    host1, host2, service, env = _activate_svc_w_scale_two(new_context,
                                                           random_str())

    # 1. verify that instances got created
    _validate_svc_instance_map_count(client, service, "active", 2)

    # 2. delete service and validate that the instance mappings are gone
    client.wait_success(service.remove())
    _validate_svc_instance_map_count(client, service, "active", 0)


def test_targets(super_client, client, context):
    env = _create_stack(client)

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
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    _validate_lb_svc_activate(env,
                              lb_svc, client, ['8081:8081', '909:1001'])

    # map web service to lb service - early binding,
    # before web service is activated
    service_link = {"serviceId": web_service.id, "ports": ["a.com:90"]}
    lb_svc = lb_svc.addservicelink(serviceLink=service_link)
    maps = _validate_svc_instance_map_count(client, lb_svc, "active", 1)
    lb_instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    agent_id = lb_instance.agentId
    item_before = _get_config_item(super_client, agent_id)

    # activate web service
    web_service = client.wait_success(web_service.activate(), 120)
    assert web_service.state == "active"
    db_service = client.wait_success(db_service.activate(), 120)
    assert db_service.state == "active"
    _validate_config_item_update(super_client, item_before, agent_id)

    # bind db and lb services after service is activated
    item_before = _get_config_item(super_client, agent_id)
    service_link = {"serviceId": db_service.id, "ports": ["a.com:90"]}
    lb_svc.addservicelink(serviceLink=service_link)
    _validate_config_item_update(super_client, item_before, agent_id)

    _validate_add_service_link(client, lb_svc, db_service,
                               ports=["a.com:90"])
    _validate_add_service_link(client, lb_svc, web_service,
                               ports=["a.com:90"])

    # remove link and make sure that the target map is gone
    item_before = _get_config_item(super_client, agent_id)
    service_link = {"serviceId": db_service.id, "ports": ["a.com:90"]}
    lb_svc.removeservicelink(serviceLink=service_link)
    _validate_config_item_update(super_client, item_before, agent_id)


def test_restart_stack(client, context):
    env = _create_stack(client)

    # create lb and web services
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    web_service = client. \
        create_service(name=random_str() + "web",
                       environmentId=env.id,
                       launchConfig=launch_config)

    web_service = client.wait_success(web_service)

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [8051, '808:1001']}
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"

    # map web service to lb service
    service_link = {"serviceId": web_service.id, "ports": ["a.com:90"]}
    lb_svc = lb_svc.addservicelink(serviceLink=service_link)

    env = client.wait_success(env.activateservices())
    lb_svc = client.wait_success(lb_svc, 120)
    assert lb_svc.state == 'active'
    web_svc = client.wait_success(lb_svc)
    assert web_svc.state == 'active'

    env = client.wait_success(env.deactivateservices())
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == 'inactive'
    web_svc = client.wait_success(web_svc)
    assert web_svc.state == 'inactive'

    env = client.wait_success(env.activateservices())
    lb_svc = client.wait_success(lb_svc, 120)
    assert lb_svc.state == 'active'
    web_svc = client.wait_success(lb_svc)
    assert web_svc.state == 'active'


def test_internal_lb(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid

    lb_launch_config = {"imageUuid": image_uuid,
                        "expose": [8051]}
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate())
    assert lb_svc.state == 'active'


def _validate_config_item_update(super_client, bf, agent_id):
    wait_for(
        lambda: find_one(super_client.list_config_item_status,
                         agentId=agent_id,
                         name='haproxy').requestedVersion > bf.requestedVersion
    )


def _get_config_item(super_client, agent_id):
    return find_one(super_client.list_config_item_status,
                    agentId=agent_id,
                    name='haproxy')


def test_target_ips(super_client, client, context):
    host = context.host
    user_account_id = host.accountId
    env = _create_stack(client)

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
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=lb_launch_config,
                                   accountId=user_account_id)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"

    # map web service to lb service - early binding,
    # before services are activated
    service_link = {"serviceId": web_service.id, "ports": ["a.com:90"]}
    lb_svc = lb_svc.addservicelink(serviceLink=service_link)

    # activate web and lb services
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    _validate_lb_svc_activate(env,
                              lb_svc, client, ['1010:1010', '111:111'])
    web_service = client.wait_success(web_service.activate(), 120)
    assert web_service.state == "active"
    db_service = client.wait_success(db_service.activate(), 120)
    assert db_service.state == "active"

    maps = _validate_svc_instance_map_count(client, lb_svc, "active", 1)
    lb_instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    agent_id = lb_instance.agentId
    item_before = _get_config_item(super_client, agent_id)

    # bind db and lb services after service is activated
    service_link = {"serviceId": db_service.id, "ports": ["a.com:90"]}
    lb_svc.addservicelink(serviceLink=service_link)
    _validate_config_item_update(super_client, item_before, agent_id)

    # remove link and make sure that the db targets are gone
    item_before = _get_config_item(super_client, agent_id)
    service_link = {"serviceId": db_service.id, "ports": ["a.com:90"]}
    lb_svc.removeservicelink(serviceLink=service_link)
    _validate_config_item_update(super_client, item_before, agent_id)

    # remove web service and validate that the web targets are gone
    item_before = _get_config_item(super_client, agent_id)
    client.wait_success(web_service.remove())
    _validate_config_item_update(super_client, item_before, agent_id)


def test_create_svc_with_lb_config(context, client):
    name = random_str()
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    app_policy = {"name": "policy1", "cookie": "cookie1",
                  "maxLength": 4, "prefix": "true",
                  "requestLearn": "false", "timeout": 10,
                  "mode": "query_string"}

    lb_policy = {"name": "policy2", "cookie": "cookie1",
                 "domain": ".test.com", "indirect": "true",
                 "nocache": "true", "postonly": "true",
                 "mode": "insert"}
    haproxy_cfg = {"defaults": "balance first", "global": "group haproxy"}
    lb_config = {"name": name,
                 "appCookieStickinessPolicy": app_policy,
                 "lbCookieStickinessPolicy": lb_policy,
                 "haproxyConfig": haproxy_cfg}

    # create service
    service = client. \
        create_loadBalancerService(name=name,
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   loadBalancerConfig=lb_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    assert service.loadBalancerConfig is not None

    # verify the load balancer config info
    config = service.loadBalancerConfig

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

    assert config.haproxyConfig == haproxy_cfg

    compose_config = env.exportconfig()
    assert compose_config is not None
    rancher_yml = yaml.load(compose_config.rancherComposeConfig)
    cfg = 'load_balancer_config'
    assert cfg in rancher_yml[service.name]
    assert 'haproxy_config' in rancher_yml[service.name][cfg]
    assert rancher_yml[service.name][cfg]["haproxy_config"] == haproxy_cfg


def test_scale(new_context):
    client = new_context.client
    register_simulated_host(new_context)
    env = _create_stack(client)
    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8081, '909:1001']}
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    service = client.create_loadBalancerService(name=random_str(),
                                                environmentId=env.id,
                                                launchConfig=launch_config,
                                                defaultCertificateId=cert1.id)
    service = client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    # validate that one instance map was created
    _validate_svc_instance_map_count(client, service, "active", 1)
    # scale up
    service = client.update(service, scale=2, name=service.name,
                            defaultCertificateId=cert2.id)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 2
    _validate_svc_instance_map_count(client, service, "active", 2)
    instance_service_map = client \
        .list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 2
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)
    wait_for_condition(
        client, instance_service_map[1], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    # now scale down
    service = client.update(service, scale=0, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 0
    _validate_svc_instance_map_count(client, service, "active", 0)


def test_labels(super_client, client, context):
    env = _create_stack(client)

    # create lb_svc with labels, and validate all of them
    # plus lb_svc label were set
    service_name = random_str()
    initial_labels = {'affinity': "container==B", '!affinity': "container==C"}
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8010, '913:913'], "labels": initial_labels}

    lb_svc = client. \
        create_loadBalancerService(name=service_name,
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    lb_svc = _validate_lb_svc_activate(env, lb_svc,
                                       client, ['8010:8010', '913:913'])
    maps = _validate_svc_instance_map_count(client, lb_svc, "active", 1)
    lb_instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    result_labels = {'affinity': "container==B", '!affinity': "container==C",
                     'io.rancher.stack_service.name':
                         env.name + "/" + service_name}

    assert all(item in lb_instance.labels.items()
               for item in result_labels.items()) is True

    # create lb_svc w/o labels, and validate that
    # only one lb_svc label was set
    service_name = random_str()
    launch_config = {"imageUuid": image_uuid,
                     "ports": [8089, '914:914']}

    lb_svc = client. \
        create_loadBalancerService(name=service_name,
                                   environmentId=env.id,
                                   launchConfig=launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    lb_svc = _validate_lb_svc_activate(env, lb_svc,
                                       client, ['8089:8089', '914:914'])
    maps = _validate_svc_instance_map_count(client, lb_svc, "active", 1)
    lb_instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    name = env.name + '/' + service_name
    result_labels = {'io.rancher.stack_service.name': name}
    assert all(item in lb_instance.labels.items()
               for item in result_labels.items()) is True


def test_inactive_lb(super_client, client, context):
    env = _create_stack(client)

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

    # map web service to lb service; validate no lb targets were created
    service_link = {"serviceId": web_service.id, "ports": ["a.com:90"]}
    lb_service = lb_service.addservicelink(serviceLink=service_link)

    # activate lb service
    lb_service = client.wait_success(lb_service.activate(), 120)
    assert lb_service.state == "active"
    maps = _validate_svc_instance_map_count(client, lb_service, "active", 1)
    lb_instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    agent_id = lb_instance.agentId
    item_before = _get_config_item(super_client, agent_id)

    # deactivate lb service, and remove service link
    lb_service = client.wait_success(lb_service.deactivate(), 120)
    assert lb_service.state == "inactive"
    service_link = {"serviceId": web_service.id, "ports": ["a.com:90"]}
    lb_service = lb_service.removeservicelink(serviceLink=service_link)
    lb_service = client.wait_success(lb_service.activate(), 120)
    assert lb_service.state == "active"
    _validate_config_item_update(super_client, item_before, agent_id)


def test_destroy_svc_instance(super_client, context, client, image_uuid):
    env = _create_stack(client)

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
    service = _validate_lb_svc_activate(env, service,
                                        client, ['94:94', '95:95'])
    maps = _validate_svc_instance_map_count(client, service, "active", 1)

    instance = _wait_for_instance_start(super_client, maps[0].instanceId)
    client.wait_success(client.delete(instance))
    _validate_svc_instance_map_count(client, service, "active", 0)

    client.wait_success(service)
    _validate_svc_instance_map_count(client, service, "active", 1)


def test_set_service_links(client, context):
    env1 = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    lb_service = client.create_loadBalancerService(name="lb",
                                                   environmentId=env1.id,
                                                   launchConfig=launch_config)
    lb_service = client.wait_success(lb_service)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    service3 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service3 = client.wait_success(service3)

    # set service2, service3 links for lb service
    service_link1 = {"serviceId": service2.id, "ports": ["a.com:90"]}
    service_link2 = {"serviceId": service3.id, "ports": ["a.com:90"]}
    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_add_service_link(client, lb_service, service2,
                               ports=["a.com:90"])
    _validate_add_service_link(client, lb_service, service3,
                               ports=["a.com:90"])

    # update the link with new ports
    service_link1 = {"serviceId": service2.id, "ports": ["a.com:100"]}
    service_link2 = {"serviceId": service3.id, "ports": ["a.com:101"]}
    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_add_service_link(client, lb_service, service2,
                               ports=["a.com:100"])
    _validate_add_service_link(client, lb_service, service3,
                               ports=["a.com:101"])

    # remove link for service3 from the list of links
    service_link = {"serviceId": service2.id, "ports": ["a.com:100"]}
    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link])
    _validate_remove_service_link(client, lb_service, service3, 1)

    # try to set duplicated service links
    with pytest.raises(ApiError) as e:
        lb_service = lb_service. \
            setservicelinks(serviceLinks=[service_link, service_link])
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'

    # set empty service link set
    lb_service = lb_service.setservicelinks(serviceLinks=[])
    _validate_remove_service_link(client, lb_service, service2, 1)


def test_modify_link(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    lb_service = client.create_loadBalancerService(name="lb",
                                                   environmentId=env.id,
                                                   launchConfig=launch_config)
    lb_service = client.wait_success(lb_service)

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)

    # set service link with hostname 1
    service_link = {"serviceId": service.id, "ports": ["a.com:90"]}
    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link])
    _validate_add_service_link(client, lb_service, service, ports=["a.com:90"])

    # update the link with new ports
    service_link = {"serviceId": service.id, "ports": ["b.com:100"]}
    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link])
    _validate_add_service_link(client, lb_service,
                               service, ports=["b.com:100"])


def _create_service(client, env, launch_config, name=None):
    if name:
        svc_name = name
    else:
        svc_name = random_str()
    service1 = client.create_service(name=svc_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    return service1


def test_create_links(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    lb_service = client.create_loadBalancerService(name="lb",
                                                   environmentId=env.id,
                                                   launchConfig=launch_config)
    lb_service = client.wait_success(lb_service)

    service1 = _create_service(client, env, launch_config)
    service2 = _create_service(client, env, launch_config)
    service3 = _create_service(client, env, launch_config)
    service4 = _create_service(client, env, launch_config)
    service5 = _create_service(client, env, launch_config)
    service6 = _create_service(client, env, launch_config)
    service7 = _create_service(client, env, launch_config)
    service8 = _create_service(client, env, launch_config)
    service9 = _create_service(client, env, launch_config)
    service10 = _create_service(client, env, launch_config)
    service11 = _create_service(client, env, launch_config)
    service12 = _create_service(client, env, launch_config)
    service13 = _create_service(client, env, launch_config)
    service14 = _create_service(client, env, launch_config)
    service15 = _create_service(client, env, launch_config)
    service16 = _create_service(client, env, launch_config)

    # set service link with hostname 1
    port1 = "example.com:80/path=81"
    port2 = "example.com"
    port3 = "example.com:80"
    port4 = "example.com:80/path"
    port5 = "example.com:80=81"
    port6 = "example.com/path"
    port7 = "example.com/path=81"
    port8 = "example.com=81"
    port9 = "80/path"
    port10 = "80/path=81"
    port11 = "80=81"
    port12 = "/path"
    port13 = "/path=81"
    port14 = "81"
    port15 = "example.com/path1/path2/path3=81"
    # old style
    port16 = "90:a.com/hello"
    service_link1 = {"serviceId": service1.id, "ports": [port1]}
    service_link2 = {"serviceId": service2.id, "ports": [port2]}
    service_link3 = {"serviceId": service3.id, "ports": [port3]}
    service_link4 = {"serviceId": service4.id, "ports": [port4]}
    service_link5 = {"serviceId": service5.id, "ports": [port5]}
    service_link6 = {"serviceId": service6.id, "ports": [port6]}
    service_link7 = {"serviceId": service7.id, "ports": [port7]}
    service_link8 = {"serviceId": service8.id, "ports": [port8]}
    service_link9 = {"serviceId": service9.id, "ports": [port9]}
    service_link10 = {"serviceId": service10.id, "ports": [port10]}
    service_link11 = {"serviceId": service11.id, "ports": [port11]}
    service_link12 = {"serviceId": service12.id, "ports": [port12]}
    service_link13 = {"serviceId": service13.id, "ports": [port13]}
    service_link14 = {"serviceId": service14.id, "ports": [port14]}
    service_link15 = {"serviceId": service15.id, "ports": [port15]}
    service_link16 = {"serviceId": service16.id, "ports": [port16]}

    lb_service = lb_service. \
        setservicelinks(serviceLinks=[service_link1, service_link2,
                                      service_link3, service_link4,
                                      service_link5, service_link6,
                                      service_link7, service_link8,
                                      service_link9, service_link10,
                                      service_link11, service_link12,
                                      service_link13, service_link14,
                                      service_link15, service_link16])
    _validate_add_service_link(client, lb_service, service1, ports=[port1])
    _validate_add_service_link(client, lb_service, service2, ports=[port2])
    _validate_add_service_link(client, lb_service, service3, ports=[port3])
    _validate_add_service_link(client, lb_service, service4, ports=[port4])
    _validate_add_service_link(client, lb_service, service5, ports=[port5])
    _validate_add_service_link(client, lb_service, service6, ports=[port6])
    _validate_add_service_link(client, lb_service, service7, ports=[port7])
    _validate_add_service_link(client, lb_service, service8, ports=[port8])
    _validate_add_service_link(client, lb_service, service9, ports=[port9])
    _validate_add_service_link(client, lb_service, service10, ports=[port10])
    _validate_add_service_link(client, lb_service, service11, ports=[port11])
    _validate_add_service_link(client, lb_service, service12, ports=[port12])
    _validate_add_service_link(client, lb_service, service13, ports=[port13])
    _validate_add_service_link(client, lb_service, service14, ports=[port14])
    _validate_add_service_link(client, lb_service, service15, ports=[port15])
    _validate_add_service_link(client, lb_service, service16, ports=[port16])

    service_link1 = {"serviceId": service1.id, "ports": ["90=100=100"]}
    with pytest.raises(ApiError) as e:
        lb_service. \
            setservicelinks(serviceLinks=[service_link1])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidPort'

    service_link1 = {"serviceId": service1.id, "ports": ["a.com:b.com:80"]}
    with pytest.raises(ApiError) as e:
        lb_service. \
            setservicelinks(serviceLinks=[service_link1])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidPort'


def test_export_config(client, context):
    env1 = _create_stack(client)

    env2 = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    web_service = _create_service(client, env1, launch_config, "web")

    web_service1 = _create_service(client, env1, launch_config, "web1")

    web_external = _create_service(client, env2, launch_config, "web2")

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [8081, '909:1001']}
    lb_service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env1.id,
                                   launchConfig=lb_launch_config)
    lb_service = client.wait_success(lb_service)
    assert lb_service.state == "inactive"

    # map web services
    service_link = {"serviceId": web_service.id,
                    "ports": ["a.com:90"], "name": "test"}
    service_link1 = {"serviceId": web_service1.id}
    service_link_ext = {"serviceId": web_external.id, "ports": ["a.com:90"]}
    lb_service = lb_service.addservicelink(serviceLink=service_link)
    lb_service = lb_service.addservicelink(serviceLink=service_link1)
    lb_service = lb_service.addservicelink(serviceLink=service_link_ext)
    compose_config = env1.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.dockerComposeConfig)
    assert len(document[lb_service.name]['links']) == 2
    assert len(document[lb_service.name]['external_links']) == 1
    assert len(document[lb_service.name]['labels']) == 2

    labels = {"io.rancher.loadbalancer.target.web": "a.com:90",
              "io.rancher.loadbalancer.target." +
              env2.name + "/web2": "a.com:90"}
    links = ["web:web", "web1:web1"]
    external_links = [env2.name + "/web2:web2"]
    assert document[lb_service.name]['labels'] == labels
    assert document[lb_service.name]['links'] == links
    assert document[lb_service.name]['external_links'] == external_links


def test_lb_service_w_certificate(client, context, image_uuid):
    env = _create_stack(client)
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    labels = {'io.rancher.loadbalancer.ssl.ports': "1765,1767"}
    launch_config = {"imageUuid": image_uuid,
                     "ports": ['1765:1766', '1767:1768'],
                     "labels": labels}

    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   certificateIds=[cert1.id, cert2.id],
                                   defaultCertificateId=cert1.id)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    # perform validation
    lb_svc = _validate_lb_svc_activate(env, lb_svc, client,
                                       ['1765:1766', '1767:1768'], "https")
    assert lb_svc.defaultCertificateId == cert1.id
    assert lb_svc.certificateIds == [cert1.id, cert2.id]

    # remove the lb_svc
    lb_svc = client.wait_success(lb_svc.remove())
    assert lb_svc.state == 'removed'

    # remove the cert
    cert1 = client.wait_success(cert1.remove())
    assert cert1.state == 'removed'
    cert2 = client.wait_success(cert2.remove())
    assert cert2.state == 'removed'


def test_lb_service_update_certificate(client, context, image_uuid):
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    labels = {'io.rancher.loadbalancer.ssl.ports': "1769,1771"}

    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid,
                     "ports": ['1769:1770', '1771:1772'],
                     "labels": labels}

    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   certificateIds=[cert1.id, cert2.id],
                                   defaultCertificateId=cert1.id)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    # perform validation
    lb_svc = _validate_lb_svc_activate(env, lb_svc,
                                       client, ['1769:1770', '1771:1772'],
                                       "https")
    assert lb_svc.defaultCertificateId == cert1.id
    assert lb_svc.certificateIds == [cert1.id, cert2.id]

    cert3 = _create_cert(client)

    # update lb_svc with new certificate set
    lb_svc = client.update(lb_svc, certificateIds=[cert1.id],
                           defaultCertificateId=cert3.id)
    lb_svc = client.wait_success(lb_svc, 120)

    assert lb_svc.defaultCertificateId == cert3.id
    assert lb_svc.certificateIds == [cert1.id]

    compose_config = env.exportconfig()
    assert compose_config is not None
    docker_compose = yaml.load(compose_config.dockerComposeConfig)
    rancher_compose = yaml.load(compose_config.rancherComposeConfig)

    assert docker_compose[lb_svc.name]['labels'] == labels
    assert rancher_compose[lb_svc.name]['default_cert'] == cert3.name
    assert rancher_compose[lb_svc.name]['certs'][0] == cert1.name

    # don't pass certificate ids and validate that they are still set
    lb_svc = client.update(lb_svc, name='newName')
    lb_svc = client.wait_success(lb_svc, 120)

    assert lb_svc.defaultCertificateId == cert3.id
    assert lb_svc.certificateIds == [cert1.id]

    # swap default and optional
    lb_svc = client.update(lb_svc, certificateIds=[cert3.id],
                           defaultCertificateId=cert1.id)
    lb_svc = client.wait_success(lb_svc)

    assert lb_svc.defaultCertificateId == cert1.id
    assert lb_svc.certificateIds == [cert3.id]

    # update with none certificates
    lb_svc = client.update(lb_svc, certificateIds=None,
                           defaultCertificateId=None)
    lb_svc = client.wait_success(lb_svc, 120)

    assert lb_svc.defaultCertificateId is None
    assert lb_svc.certificateIds is None


def test_lb_with_certs_service_update(new_context, image_uuid):
    client = new_context.client
    new_context.host
    register_simulated_host(new_context)
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    labels = {'io.rancher.loadbalancer.ssl.ports': "1772,1773"}

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid,
                     "ports": ['1792', '1793'],
                     "labels": labels}

    service = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   certificateIds=[cert1.id, cert2.id],
                                   defaultCertificateId=cert1.id,
                                   scale=2)
    service = client.wait_success(service)
    assert service.state == "inactive"
    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    assert service.defaultCertificateId == cert1.id
    assert service.certificateIds == [cert1.id, cert2.id]

    # scale down service and validate the certificates are still the same
    service = client.update(service, scale=1)
    service = client.wait_success(service)
    assert service.state == 'active'

    assert service.defaultCertificateId == cert1.id
    assert service.certificateIds == [cert1.id, cert2.id]


def test_cert_in_use(client, context, image_uuid):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    labels = {'io.rancher.loadbalancer.ssl.ports': "1765,1767"}
    launch_config = {"imageUuid": image_uuid,
                     "ports": ['1765:1766', '1767:1768'],
                     "labels": labels}

    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env.id,
                                   launchConfig=launch_config,
                                   certificateIds=[cert1.id, cert2.id],
                                   defaultCertificateId=cert1.id)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = client.wait_success(lb_svc.activate(), 120)
    # perform validation
    lb_svc = _validate_lb_svc_activate(env, lb_svc,
                                       client, ['1765:1766', '1767:1768'],
                                       "https")
    assert lb_svc.defaultCertificateId == cert1.id
    assert lb_svc.certificateIds == [cert1.id, cert2.id]

    # try to remove the cert - delete action (used by UI)
    with pytest.raises(ApiError) as e:
        client.delete(cert1)
    assert e.value.error.status == 405
    assert e.value.error.code == 'InvalidAction'

    # try to remove the cert - remove action
    with pytest.raises(ApiError) as e:
        cert1.remove()
    assert e.value.error.status == 405
    assert e.value.error.code == 'InvalidAction'


def test_concurrent_acitvate_setlinks(client, context):
    env1 = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    svc = _create_service(client, env1, launch_config, "web")

    lb_launch_config = {"imageUuid": image_uuid,
                        "ports": [8777, '8778:8778']}
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   environmentId=env1.id,
                                   launchConfig=lb_launch_config)
    lb_svc = client.wait_success(lb_svc)

    svc.activate()
    lb_svc.activate()
    # map web services
    service_link = {"serviceId": svc.id,
                    "ports": ["a.com:90"], "name": "test"}
    lb_svc.addservicelink(serviceLink=service_link)
    lb_svc = client.wait_success(lb_svc)
    svc = client.wait_success(svc)

    # validate that the instance was created
    _validate_svc_instance_map_count(client, lb_svc, "active", 1)

    # update the link
    service_link = {"serviceId": svc.id,
                    "ports": ["a.com:100"], "name": "test"}
    lb_svc.addservicelink(serviceLink=service_link)
    _validate_svc_instance_map_count(client, lb_svc, "active", 1)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def _validate_lb_svc_activate(env, service, client, ports, protocol=None):
    # 1. verify that the service was activated
    assert service.state == "active"
    # 2. verify instance got created
    _validate_svc_instance_map_count(client,
                                     service, "active", service.scale)
    return service


def _activate_svc_w_scale_two(new_context, random_str):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    env = _create_stack(client)
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

    return host1, host2, service, env


def _validate_add_service_link(client, service, consumedService, ports=None):
    service_maps = client. \
        list_serviceConsumeMap(serviceId=service.id,
                               consumedServiceId=consumedService.id)

    assert len(service_maps) == 1

    if ports:
        for value in service_maps:
            if value.ports == ports:
                service_map = value
                break

    assert service_map is not None


def _validate_remove_service_link(client, service, consumedService, count,
                                  timeout=30):
    start = time.time()
    service_maps = client. \
        list_serviceConsumeMap(serviceId=service.id,
                               consumedServiceId=consumedService.id,
                               state='removed')
    while len(service_maps) != count:
        time.sleep(.5)
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id,
                                   state='removed')
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be removed.'


def _create_cert(client):
    cert = _read_cert("cert.pem")
    key = _read_cert("key.pem")
    cert1 = client. \
        create_certificate(name=random_str(),
                           cert=cert,
                           key=key)
    cert1 = client.wait_success(cert1)
    assert cert1.state == 'active'
    assert cert1.cert == cert
    return cert1


def _read_cert(name):
    with open(os.path.join(RESOURCE_DIR, name)) as f:
        return f.read()


def _validate_svc_instance_map_count(client, service,
                                     state, count, timeout=30):
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state=state)
    while len(instance_service_map) < count:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id, state=state)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be in correct state'

    return instance_service_map


def _wait_for_instance_start(super_client, id):
    wait_for(
        lambda: len(super_client.by_id('container', id)) > 0
    )
    return super_client.by_id('container', id)


def _create_stack(client):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env
