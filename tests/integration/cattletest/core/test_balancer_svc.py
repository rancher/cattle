from common_fixtures import *  # NOQA
from cattle import ApiError
import yaml


@pytest.fixture(scope='module')
def image_uuid(context):
    return context.image_uuid


def _create_stack(client):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def test_validate_balancer_svc_fields(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    service_id = svc.id
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port, "priority": priority,
                 "serviceId": service_id,
                 "targetPort": target_port,
                 "backendName": backend_name}
    port_rules = [port_rule]
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    policy = {"name": "policy2", "cookie": "cookie1",
              "domain": ".test.com", "indirect": "true",
              "nocache": "true", "postonly": "true",
              "mode": "insert"}

    lb_config = {"portRules": port_rules, "defaultCertificateId": cert1.id,
                 "certificateIds": [cert2.id],
                 "config": config, "stickinessPolicy": policy}

    # create service
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.launchConfig.imageUuid == image_uuid

    lb = lb_svc.lbConfig
    assert lb is not None

    assert lb.defaultCertificateId == cert1.id
    assert lb.certificateIds == [cert2.id]
    assert lb.config == config
    assert len(lb.portRules) == 1
    rule = lb.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.sourcePort == port
    assert rule.priority == priority
    assert rule.protocol == "http"
    assert rule.serviceId is not None
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name
    assert lb.stickinessPolicy.name == "policy2"
    assert lb.stickinessPolicy.cookie == "cookie1"
    assert lb.stickinessPolicy.domain == ".test.com"
    assert lb.stickinessPolicy.indirect is True
    assert lb.stickinessPolicy.nocache is True
    assert lb.stickinessPolicy.postonly is True
    assert lb.stickinessPolicy.mode == "insert"

    compose_config = env.exportconfig()

    assert compose_config is not None
    docker_yml = yaml.load(compose_config.dockerComposeConfig)
    y = yaml.load(compose_config.rancherComposeConfig)
    assert "lb_config" in y['services'][lb_svc.name]
    lb = y['services'][lb_svc.name]["lb_config"]
    assert 'metadata' not in y['services'][lb_svc.name]
    assert lb is not None
    assert len(lb["port_rules"]) == 1
    rule = lb["port_rules"][0]
    assert rule["hostname"] == hostname
    assert rule["path"] == path
    assert rule["source_port"] == port
    assert rule["priority"] == priority
    assert rule["protocol"] == "http"
    assert rule["service"] == svc.name
    assert rule["target_port"] == target_port
    assert rule["backend_name"] == backend_name
    assert lb["default_cert"] == cert1.name
    assert lb["certs"] == [cert2.name]
    assert lb["config"] == config
    assert lb["stickiness_policy"]["name"] == "policy2"
    assert lb["stickiness_policy"]["cookie"] == "cookie1"
    assert lb["stickiness_policy"]["domain"] == ".test.com"
    assert lb["stickiness_policy"]["indirect"] is True
    assert lb["stickiness_policy"]["nocache"] is True
    assert lb["stickiness_policy"]["postonly"] is True
    assert lb["stickiness_policy"]["mode"] == "insert"
    assert "links" not in docker_yml['services'][lb_svc.name]

    # try to remove certificate
    with pytest.raises(ApiError) as e:
        cert1.remove()
    assert e.value.error.status == 405
    assert e.value.error.code == 'InvalidAction'

    # delete balancer service
    client.wait_success(lb_svc.remove())
    cert1.remove()
    cert2.remove()


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


RESOURCE_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            'resources/certs')


def test_validation_create_balancer(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp"}
        port_rules = [port_rule]
        lb_config = {"portRules": port_rules}
        client. \
            create_loadBalancerService(name=random_str(),
                                       stackId=env.id,
                                       launchConfig=launch_config,
                                       lbConfig=lb_config)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "serviceId"

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp",
                     "selector": "foo=bar", "serviceId": svc.id}
        port_rules = [port_rule]
        lb_config = {"portRules": port_rules}

        client. \
            create_loadBalancerService(name=random_str(),
                                       stackId=env.id,
                                       launchConfig=launch_config,
                                       lbConfig=lb_config)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp", "serviceId": svc.id}
        port_rules = [port_rule]
        lb_config = {"portRules": port_rules}

        client. \
            create_loadBalancerService(name=random_str(),
                                       stackId=env.id,
                                       launchConfig=launch_config,
                                       lbConfig=lb_config)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "targetPort"


def test_activate_balancer(client, super_client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    lb_launch_config = {"ports": [8289],
                        "imageUuid": image_uuid}
    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    service_id = svc.id
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port, "priority": priority,
                 "protocol": protocol, "serviceId": service_id,
                 "targetPort": target_port,
                 "backendName": backend_name}
    port_rules = [port_rule]
    policy = {"name": "policy2", "cookie": "cookie1",
              "domain": ".test.com", "indirect": "true",
              "nocache": "true", "postonly": "true",
              "mode": "insert"}

    lb_config = {"portRules": port_rules,
                 "config": config, "stickinessPolicy": policy}

    # create service
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=lb_launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.launchConfig.ports == ["8289:8289/tcp"]

    # activate service
    lb_svc = lb_svc.activate()
    maps = _wait_until_active_map_count(lb_svc, 1, client)
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    assert c.ports == ["8289:8289/tcp"]
    assert c.imageUuid is not None
    _update_healthy(super_client, c)
    # validate service link got created for the target service
    _validate_add_service_link(lb_svc, svc, client)
    lb_svc = client.wait_success(lb_svc)

    # update ports rules to the new service, validate links update
    svc1 = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc1 = client.wait_success(svc1)
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port, "priority": priority,
                 "protocol": protocol, "serviceId": svc1.id,
                 "targetPort": target_port,
                 "backendName": backend_name}
    port_rules = [port_rule]
    lb_config = {"portRules": port_rules}
    lb_svc = client.update(lb_svc, lbConfig=lb_config)
    _validate_add_service_link(lb_svc, svc1, client)
    _validate_remove_service_link(lb_svc, svc, client)
    assert len(lb_svc.lbConfig.portRules) == 1

    # remove svc1, validate the link is gone
    client.wait_success(svc1.remove())
    _validate_remove_service_link(lb_svc, svc1, client)


def test_validate_svc_fields(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}
    hostname = "foo"
    path = "bar"
    target_port = 42
    backend_name = "myBackend"
    port_rule = {"hostname": hostname,
                 "path": path,
                 "targetPort": target_port,
                 "backendName": backend_name}
    port_rules = [port_rule]

    lb_config = {"portRules": port_rules}
    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config,
                       lbConfig=lb_config)

    svc = client.wait_success(svc)

    assert svc.state == "inactive"
    assert svc.launchConfig.imageUuid == image_uuid
    assert len(svc.lbConfig.portRules) == 1
    rule = svc.lbConfig.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name

    hostname = "foo1"
    path = "bar1"
    target_port = 43
    backend_name = "myBackend1"
    port_rule = {"hostname": hostname,
                 "path": path,
                 "targetPort": target_port,
                 "backendName": backend_name}
    lb_config = {"portRules": [port_rule]}
    svc = client.update(svc, lbConfig=lb_config)
    svc = client.wait_success(svc)
    assert len(svc.lbConfig.portRules) == 1
    rule = svc.lbConfig.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name

    compose_config = env.exportconfig()

    assert compose_config is not None
    docker_yml = yaml.load(compose_config.rancherComposeConfig)
    assert 'metadata' not in docker_yml['services'][svc.name]
    y = yaml.load(compose_config.rancherComposeConfig)
    assert "lb_config" in y['services'][svc.name]
    lb = y['services'][svc.name]["lb_config"]
    assert lb is not None
    assert len(lb["port_rules"]) == 1
    rule = lb["port_rules"][0]
    assert rule["hostname"] == hostname
    assert rule["path"] == path
    assert rule["target_port"] == target_port
    assert "source_port" not in rule
    assert "priority" not in rule
    assert "protocol" not in rule
    assert "service" not in rule


def test_svc_remove(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc1 = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc1 = client.wait_success(svc1)

    svc2 = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc2 = client.wait_success(svc2)

    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule1 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "serviceId": svc1.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rule2 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "serviceId": svc2.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rules = [port_rule1, port_rule2]
    lb_config = {"portRules": port_rules,
                 "config": config}

    # create balancer
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.lbConfig.portRules) == 2

    # remove one of the services
    client.wait_success(svc1.remove())

    def wait_for_port_count(service):
        lb = client. \
            reload(lb_svc)
        return len(lb.lbConfig.portRules) == 1

    wait_for(lambda: wait_for_condition(client, lb_svc, wait_for_port_count))


def test_service_selector(client, image_uuid):
    env = _create_stack(client)

    labels = {'foo': "bar"}
    launch_config = {"imageUuid": image_uuid, "labels": labels}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    lb_launch_config = {"ports": [8289],
                        "imageUuid": image_uuid}
    hostname = "foo"
    path = "bar"
    port = 32
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port,
                 "selector": "foo=bar"}
    port_rules = [port_rule]

    lb_config = {"portRules": port_rules}

    # create service
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=lb_launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)

    # validate service link got created for the target service
    _validate_add_service_link(lb_svc, svc, client)

    # update service, validate link is done
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port,
                 "selector": "foo=bar1"}
    port_rules = [port_rule]

    lb_config = {"portRules": port_rules}
    lb_svc = client.update(lb_svc, lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    _validate_remove_service_link(lb_svc, svc, client)

    # update service back, validate link is back
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port,
                 "selector": "foo=bar"}
    port_rules = [port_rule]

    lb_config = {"portRules": port_rules}
    lb_svc = client.update(lb_svc, lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    _validate_add_service_link(lb_svc, svc, client)

    # remove service, validate link is gone
    client.wait_success(svc.remove())
    _validate_remove_service_link(lb_svc, svc, client)

    # add service back, make sure the link is recreated
    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)
    _validate_add_service_link(lb_svc, svc, client)


def test_validate_export_cross_stack(client, image_uuid):
    env1 = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env1.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    hostname = "foo"
    path = "bar"
    port = 32
    service_id = svc.id
    target_port = 42
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port,
                 "serviceId": service_id,
                 "targetPort": target_port}

    lb_config = {"portRules": [port_rule]}

    # create service
    env2 = _create_stack(client)
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env2.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)

    compose_config = env2.exportconfig()
    assert compose_config is not None
    docker_yml = yaml.load(compose_config.rancherComposeConfig)
    assert 'metadata' not in docker_yml['services'][lb_svc.name]
    y = yaml.load(compose_config.rancherComposeConfig)
    assert "lb_config" in y['services'][lb_svc.name]
    lb = y['services'][lb_svc.name]["lb_config"]
    assert lb is not None
    assert len(lb["port_rules"]) == 1
    rule = lb["port_rules"][0]
    assert rule["service"] == env1.name + "/" + svc.name


def test_lb_noop(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}
    svc1 = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc1 = client.wait_success(svc1)

    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule1 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "serviceId": svc1.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rules = [port_rule1]
    lb_config = {"portRules": port_rules,
                 "config": config}

    # create balancer
    launch_config = {"imageUuid": "rancher/none"}
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.lbConfig.portRules) == 1

    client.wait_success(lb_svc.activate())
    # no instances should be created
    _wait_until_active_map_count(lb_svc, 0, client)

    # try updating endpoints, should pass
    public_e = {"ipAddress": "10.1.1.1", "port": "100"}
    lb_svc = client.update(lb_svc, publicEndpoints=[public_e])
    assert len(lb_svc.publicEndpoints) == 1


def _wait_until_active_map_count(service, count, client):
    def wait_for_map_count(service):
        m = client. \
            list_serviceExposeMap(serviceId=service.id, state='active')
        return len(m) == count

    wait_for_condition(client, service, wait_for_map_count)
    return client. \
        list_serviceExposeMap(serviceId=service.id, state='active')


def _validate_add_service_link(svc,
                               consumed_svc, client, link_name=None):
    def get_links():
        if link_name is None:
            return client. \
                list_serviceConsumeMap(serviceId=svc.id,
                                       consumedServiceId=consumed_svc.id)
        else:
            return client. \
                list_serviceConsumeMap(serviceId=svc.id,
                                       consumedServiceId=consumed_svc.id,
                                       name=link_name)

    def wait_for_link_count(svc):
        return len(get_links()) == 1

    wait_for_condition(client, svc, wait_for_link_count)

    svc_maps = get_links()
    if link_name is not None:
        assert svc_maps[0].name is not None

    svc_map = svc_maps[0]
    wait_for_condition(
        client, svc_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_service_link(service,
                                  consumedService, client, link_name=None):
    def check():
        if link_name is None:
            service_maps = client. \
                list_serviceConsumeMap(serviceId=service.id,
                                       consumedServiceId=consumedService.id)
        else:
            service_maps = client. \
                list_serviceConsumeMap(serviceId=service.id,
                                       consumedServiceId=consumedService.id,
                                       name=link_name)

        return len(service_maps) == 0

    wait_for(check)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def test_standalone_container(client, image_uuid):
    c1 = client.create_container(name=random_str(), imageUuid=image_uuid)

    env = _create_stack(client)
    launch_config = {"imageUuid": image_uuid}
    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule1 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "instanceId": c1.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rules = [port_rule1]
    lb_config = {"portRules": port_rules,
                 "config": config}

    # create balancer
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.lbConfig.portRules) == 1
    assert lb_svc.lbConfig.portRules[0].instanceId == c1.id

    # remove container
    c1 = client.wait_success(c1.stop())
    client.wait_success(c1.remove())

    def wait_for_port_count(service):
        lb = client. \
            reload(lb_svc)
        return len(lb.lbConfig.portRules) == 0

    wait_for(lambda: wait_for_condition(client, lb_svc, wait_for_port_count))


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
        env = client.create_stack(name='env-' + random_str())
        _create_and_activate_svc(client, 'global',
                                 name='agentglobal' + random_str(),
                                 launchConfig={
                                     'imageUuid': context.image_uuid,
                                     'healthCheck': {
                                         'port': 80,
                                     }, "labels": {
                                         'io.rancher.scheduler.global': 'true',
                                        'io.rancher.container.create_agent':
                                         'true'
                                     }
                                 }, stackId=env.id)

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


def _create_and_activate_svc(client, count, **kw):
    svc = client.create_service(kw)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc.activate()
    # wait for service instances to become running
    if count == 'global':
        wait_for(lambda: len(client.reload(svc).instances()) >= 1)
    else:
        wait_for(lambda: len(client.reload(svc).instances()) == count)
    for i in client.reload(svc).instances():
        wait_for(lambda: client.reload(i).state == 'running')
    return client.reload(svc)


def _agent_update_healthy(agent, hcihm, c, super_client):
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


def _update_healthy(super_client, c):
    wait_for(lambda: super_client.reload(c).state == 'running')
    c = super_client.reload(c)
    hci = find_one(c.healthcheckInstances)
    for hcihm in hci.healthcheckInstanceHostMaps():
        agent = _get_agent_for_container(context, c)
        assert hcihm.healthState == 'initializing'
        _agent_update_healthy(agent, hcihm, c, super_client)


def test_lb_service_add_instance_selector(super_client, client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid

    # use case #1 - instance having selector's label,
    # is present when service with selector is created
    labels = {'foo32': "bar"}
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True,
                                         labels=labels)
    container1 = client.wait_success(container1)
    assert container1.state == "running"

    launch_config = {"imageUuid": "rancher/none"}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                selectorContainer="foo32=bar")
    svc = client.wait_success(svc)
    assert svc.selectorContainer == "foo32=bar"
    svc = client.wait_success(svc.activate(), 120)
    assert svc.state == "active"

    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=svc.id)) == 1
    )

    # register service to lb service
    launch_config = {"imageUuid": image_uuid,
                     "ports": [567, '568:569'],
                     "expose": [9999, '9998:9997']}
    port_rule = {"hostname": "foo.com",
                 "path": "/bar", "sourcePort": 100,
                 "selector": "foo=bar"}
    port_rules = [port_rule]
    lb_config = {"portRules": port_rules}
    lb_svc = client.create_loadBalancerService(name=random_str(),
                                               stackId=env.id,
                                               launchConfig=launch_config,
                                               lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    lb_svc = lb_svc.activate()
    maps = _wait_until_active_map_count(lb_svc, 1, client)
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    _update_healthy(super_client, c)
    lb_svc = client.wait_success(lb_svc)

    service_link = {"serviceId": svc.id}
    lb_svc.addservicelink(serviceLink=service_link)

    # use case #2 - instance having selector's label,
    # is added after service with selector creation
    c2 = client.create_container(imageUuid=image_uuid,
                                 startOnCreate=True,
                                 labels=labels)
    c2 = client.wait_success(c2)
    assert c2.state == "running"
    wait_for(
        lambda: len(client.list_serviceExposeMap(serviceId=svc.id)) == 2
    )


def test_lb_svc_ports_update(client, super_client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    lb_config = {}
    launch_config = {"imageUuid": image_uuid, "ports": ['8683:8683']}
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"

    # activate service
    lb_svc = lb_svc.activate()
    maps = _wait_until_active_map_count(lb_svc, 1, client)
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    _update_healthy(super_client, c)
    lb_svc = client.wait_success(lb_svc)
    assert c.ports == ['8683:8683/tcp']
    wait_for(
        lambda: client.reload(lb_svc).publicEndpoints is not None and len(
            client.reload(lb_svc).publicEndpoints) == 1)
    endpoints = client.reload(lb_svc).publicEndpoints
    ep = endpoints[0]
    assert ep.port == 8683

    new_launch_config = {"ports": ['8684']}

    lb_svc = client.update(lb_svc, launchConfig=new_launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.launchConfig.ports == ['8684:8684/tcp']
    maps = _wait_until_active_map_count(lb_svc, 1, client)
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    assert c.ports == ['8684:8684/tcp']

    wait_for(
        lambda: client.reload(lb_svc).publicEndpoints is not None and len(
            client.reload(lb_svc).publicEndpoints) == 1)
    endpoints = client.reload(lb_svc).publicEndpoints
    ep = endpoints[0]
    assert ep.port == 8684


def test_svc_update_readd_rule(client, super_client, image_uuid):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)

    launch_config = {"imageUuid": image_uuid}
    svc1 = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc1 = client.wait_success(svc1)

    svc1 = client.wait_success(svc1.activate())

    launch_config = {"imageUuid": image_uuid, "ports": ["45677:45677"]}
    hostname = "foo"
    path = "bar"
    port = 32
    priority = 10
    protocol = "http"
    target_port = 42
    backend_name = "myBackend"
    config = "global maxconn 20"
    port_rule1 = {"hostname": hostname,
                  "path": path, "sourcePort": port, "priority": priority,
                  "protocol": protocol, "serviceId": svc1.id,
                  "targetPort": target_port,
                  "backendName": backend_name}
    port_rules = [port_rule1]
    lb_config = {"portRules": port_rules,
                 "config": config}

    # create balancer
    lb_svc = client. \
        create_loadBalancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   lbConfig=lb_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.lbConfig.portRules) == 1

    lb_svc = lb_svc.activate()
    maps = _wait_until_active_map_count(lb_svc, 1, client)
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    _update_healthy(super_client, c)
    lb_svc = client.wait_success(lb_svc)
    # update with empty ports
    launch_config = {"imageUuid": image_uuid, "ports": []}
    lb_svc = client.update(lb_svc, launchConfig=launch_config)
    lb_svc = client.wait_success(lb_svc)

    # put the ports back in
    launch_config = {"imageUuid": image_uuid, "ports": ["45677:45677"]}
    lb_svc = client.update(lb_svc, launchConfig=launch_config)
    client.wait_success(lb_svc)
