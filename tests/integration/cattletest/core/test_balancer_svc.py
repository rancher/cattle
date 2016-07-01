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
    cert1 = _create_cert(client)
    cert2 = _create_cert(client)
    policy = {"name": "policy2", "cookie": "cookie1",
              "domain": ".test.com", "indirect": "true",
              "nocache": "true", "postonly": "true",
              "mode": "insert"}

    # create service
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config,
                               portRules=port_rules,
                               defaultCertificateId=cert1.id,
                               certificateIds=[cert2.id],
                               config=config,
                               stickinessPolicy=policy)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.launchConfig.imageUuid == image_uuid
    assert lb_svc.defaultCertificateId == cert1.id
    assert lb_svc.certificateIds == [cert2.id]
    assert lb_svc.config == config
    assert len(lb_svc.portRules) == 1
    rule = lb_svc.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.sourcePort == port
    assert rule.priority == priority
    assert rule.protocol == protocol
    assert rule.serviceId is not None
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name
    assert lb_svc.stickinessPolicy.name == "policy2"
    assert lb_svc.stickinessPolicy.cookie == "cookie1"
    assert lb_svc.stickinessPolicy.domain == ".test.com"
    assert lb_svc.stickinessPolicy.indirect is True
    assert lb_svc.stickinessPolicy.nocache is True
    assert lb_svc.stickinessPolicy.postonly is True
    assert lb_svc.stickinessPolicy.mode == "insert"

    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 1
    rule = lb.port_rules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.source_port == port
    assert rule.priority == priority
    assert rule.protocol == protocol
    assert rule.service == env.name + "/" + svc.name
    assert rule.target_port == target_port
    assert rule.backend_name == backend_name
    assert lb.default_cert == cert1.name
    assert lb.certs == [cert2.name]
    assert lb.config == config
    assert lb.stickiness_policy.name == "policy2"
    assert lb.stickiness_policy.cookie == "cookie1"
    assert lb.stickiness_policy.domain == ".test.com"
    assert lb.stickiness_policy.indirect is True
    assert lb.stickiness_policy.nocache is True
    assert lb.stickiness_policy.postonly is True
    assert lb.stickiness_policy.mode == "insert"

    compose_config = env.exportconfig()

    assert compose_config is not None
    docker_yml = yaml.load(compose_config.rancherComposeConfig)
    assert 'metadata' not in docker_yml['services'][lb_svc.name]


def test_create_balancer_svc_meta(client, image_uuid):
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
    protocol = "http"
    service_id = svc.id
    target_port = 42
    backend_name = "myBackend"
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port, "priority": priority,
                 "protocol": protocol, "serviceId": service_id,
                 "targetPort": target_port,
                 "backendName": backend_name}
    port_rules = [port_rule]

    # create service
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config,
                               portRules=port_rules,
                               metadata={"foo": "bar"})
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.launchConfig.imageUuid == image_uuid
    assert len(lb_svc.portRules) == 1

    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert lb_svc.metadata["foo"] == "bar"
    assert len(lb.port_rules) == 1
    rule = lb.port_rules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.source_port == port
    assert rule.priority == priority
    assert rule.protocol == protocol
    assert rule.service == env.name + "/" + svc.name
    assert rule.target_port == target_port
    assert rule.backend_name == backend_name

    protocol = "tcp"
    port_rule = {"hostname": hostname,
                 "path": path, "sourcePort": port, "priority": priority,
                 "protocol": protocol, "serviceId": service_id,
                 "targetPort": target_port,
                 "backendName": backend_name}
    lb_svc = client.update(lb_svc, portRules=[port_rule])
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert lb_svc.metadata["foo"] == "bar"
    rule = lb.port_rules[0]
    assert rule.protocol == protocol

    lb_svc = client.update(lb_svc, metadata={"foo": "bar1"})
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert lb_svc.metadata["foo"] == "bar1"
    rule = lb.port_rules[0]
    assert rule.protocol == protocol

    lb_svc = client.update(lb_svc, portRules=[port_rule, port_rule])
    lb_svc = client.wait_success(lb_svc)
    assert len(lb_svc.portRules) == 2
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 2


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

    port = 32
    priority = 10
    protocol = "http"
    service_id = svc.id
    target_port = 42
    port_rule = {"sourcePort": port, "priority": priority,
                 "protocol": protocol, "serviceId": service_id,
                 "targetPort": target_port}
    port_rules = [port_rule]

    # create service
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config,
                               portRules=port_rules,
                               metadata={"foo": "bar"})
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.launchConfig.imageUuid == image_uuid
    assert len(lb_svc.portRules) == 1

    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert lb_svc.metadata["foo"] == "bar"
    assert len(lb.port_rules) == 1
    rule = lb.port_rules[0]
    assert rule.hostname is None
    assert rule.path is None
    assert rule.source_port == port
    assert rule.priority == priority
    assert rule.protocol == protocol
    assert rule.service == env.name + "/" + svc.name
    assert rule.target_port == target_port
    assert rule.backend_name is None
    assert rule.selector is None

    with pytest.raises(ApiError) as e:
        port_rule = {"protocol": protocol, "serviceId": service_id,
                     "targetPort": target_port}
        port_rules = [port_rule]

        client. \
            create_balancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   portRules=port_rules)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "sourcePort"

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "serviceId": service_id,
                     "targetPort": target_port}
        port_rules = [port_rule]

        client. \
            create_balancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   portRules=port_rules)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "protocol"

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp"}
        port_rules = [port_rule]

        client. \
            create_balancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   portRules=port_rules)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "serviceId"

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp",
                     "selector": "foo=bar", "serviceId": svc.id}
        port_rules = [port_rule]

        client. \
            create_balancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   portRules=port_rules)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'

    with pytest.raises(ApiError) as e:
        port_rule = {"sourcePort": 94, "protocol": "tcp", "serviceId": svc.id}
        port_rules = [port_rule]

        client. \
            create_balancerService(name=random_str(),
                                   stackId=env.id,
                                   launchConfig=launch_config,
                                   portRules=port_rules)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == "targetPort"


def test_activate_balancer(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config)

    svc = client.wait_success(svc)

    lb_launch_config = {"ports": [8289]}
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

    # create service
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=lb_launch_config,
                               portRules=port_rules,
                               config=config,
                               stickinessPolicy=policy)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert lb_svc.launchConfig.healthCheck is not None
    assert lb_svc.config == config
    assert len(lb_svc.portRules) == 1
    rule = lb_svc.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.sourcePort == port
    assert rule.priority == priority
    assert rule.protocol == protocol
    assert rule.serviceId is not None
    assert rule.targetPort == target_port
    assert lb_svc.launchConfig.ports == ["8289:8289"]

    # activate service
    lb_svc = client.wait_success(lb_svc.activate())
    assert lb_svc.state == "active"
    maps = client. \
        list_serviceExposeMap(serviceId=lb_svc.id, state='active')
    assert len(maps) == 1
    expose_map = maps[0]
    c = client.reload(expose_map.instance())
    assert c.ports == ["8289:8289"]
    assert c.imageUuid is not None


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

    svc = client. \
        create_service(name=random_str(),
                       stackId=env.id,
                       launchConfig=launch_config,
                       portRules=port_rules)

    svc = client.wait_success(svc)

    assert svc.state == "inactive"
    assert svc.launchConfig.imageUuid == image_uuid
    assert len(svc.portRules) == 1
    rule = svc.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name

    assert svc.metadata is not None
    lb = svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 1
    rule = lb.port_rules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.service == env.name + "/" + svc.name
    assert rule.target_port == target_port
    assert rule.backend_name == backend_name

    hostname = "foo1"
    path = "bar1"
    target_port = 43
    backend_name = "myBackend1"
    port_rule = {"hostname": hostname,
                 "path": path,
                 "targetPort": target_port,
                 "backendName": backend_name}
    svc = client.update(svc, portRules=[port_rule])
    svc = client.wait_success(svc)
    assert len(svc.portRules) == 1
    rule = svc.portRules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.targetPort == target_port
    assert rule.backendName == backend_name

    assert svc.metadata is not None
    lb = svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 1
    rule = lb.port_rules[0]
    assert rule.hostname == hostname
    assert rule.path == path
    assert rule.service == env.name + "/" + svc.name
    assert rule.target_port == target_port
    assert rule.backend_name == backend_name


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

    # create balancer
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config,
                               portRules=port_rules,
                               config=config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.portRules) == 2
    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 2

    # remove one of the services
    client.wait_success(svc1.remove())

    def wait_for_port_count(service):
        lb = client. \
            reload(lb_svc)
        return len(lb.portRules) == 1

    wait_for(lambda: wait_for_condition(client, lb_svc, wait_for_port_count))
    assert lb_svc.metadata is not None
    lb_svc = client.reload(lb_svc)
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 1


def test_svc_null_ports(client, image_uuid):
    env = _create_stack(client)

    launch_config = {"imageUuid": image_uuid}

    # create balancer
    lb_svc = client. \
        create_balancerService(name=random_str(),
                               stackId=env.id,
                               launchConfig=launch_config)
    lb_svc = client.wait_success(lb_svc)
    assert lb_svc.state == "inactive"
    assert len(lb_svc.portRules) == 0
    assert lb_svc.metadata is not None
    lb = lb_svc.metadata["lb"]
    assert lb is not None
    assert len(lb.port_rules) == 0
