from common_fixtures import *  # NOQA
from cattle import ApiError


def create_env_and_svc(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    return service, env


def test_activate_single_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    host = context.host
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True)
    container1 = client.wait_success(container1)

    container2 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True)
    container2 = client.wait_success(container2)

    caps = ["SYS_MODULE"]

    restart_policy = {"maximumRetryCount": 2, "name": "on-failure"}

    dns = ['8.8.8.8', '1.2.3.4']

    health_check = {"name": "check1", "responseTimeout": 3,
                    "interval": 4, "healthyThreshold": 5,
                    "unhealthyThreshold": 6, "requestLine": "index.html",
                    "port": 200}

    launch_config = {"imageUuid": image_uuid}

    consumed_service = client.create_service(name=random_str(),
                                             environmentId=env.id,
                                             launchConfig=launch_config)
    consumed_service = client.wait_success(consumed_service)

    launch_config = {"imageUuid": image_uuid,
                     "command": ['sleep', '42'],
                     "environment": {'TEST_FILE': "/etc/testpath.conf"},
                     "ports": ['8081', '8082/tcp'],
                     "dataVolumes": ['/foo'],
                     "dataVolumesFrom": [container1.id],
                     "capAdd": caps,
                     "capDrop": caps,
                     "dnsSearch": dns,
                     "dns": dns,
                     "privileged": True,
                     "domainName": "rancher.io",
                     "memory": 8000000,
                     "stdinOpen": True,
                     "tty": True,
                     "entryPoint": ["/bin/sh", "-c"],
                     "cpuShares": 400,
                     "cpuSet": "2",
                     "restartPolicy": restart_policy,
                     "directory": "/",
                     "hostname": "test",
                     "user": "test",
                     "instanceLinks": {
                         'container2_link':
                             container2.id},
                     "requestedHostId": host.id,
                     "healthCheck": health_check}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)

    # validate that parameters were set for service
    assert service.state == "inactive"
    assert service.launchConfig.imageUuid == image_uuid
    assert service.launchConfig.command == ['sleep', '42']
    assert len(service.launchConfig.environment) == 1
    assert len(service.launchConfig.ports) == 2
    assert len(service.launchConfig.dataVolumes) == 1
    # assert set(service.launchConfig.dataVolumesFrom) == set([container1.id])
    assert service.launchConfig.capAdd == caps
    assert service.launchConfig.capDrop == caps
    assert service.launchConfig.dns == dns
    assert service.launchConfig.dnsSearch == dns
    assert service.launchConfig.privileged is True
    assert service.launchConfig.domainName == "rancher.io"
    assert service.launchConfig.memory == 8000000
    assert service.launchConfig.stdinOpen is True
    assert service.launchConfig.tty is True
    assert service.launchConfig.entryPoint == ["/bin/sh", "-c"]
    assert service.launchConfig.cpuShares == 400
    assert service.launchConfig.restartPolicy == restart_policy
    assert service.launchConfig.directory == "/"
    assert service.launchConfig.hostname == "test"
    assert service.launchConfig.user == "test"
    assert len(service.launchConfig.instanceLinks) == 1
    assert service.kind == "service"
    # assert service.launchConfig.registryCredentialId == reg_cred.id
    assert service.launchConfig.healthCheck.name == "check1"
    assert service.launchConfig.healthCheck.responseTimeout == 3
    assert service.launchConfig.healthCheck.interval == 4
    assert service.launchConfig.healthCheck.healthyThreshold == 5
    assert service.launchConfig.healthCheck.unhealthyThreshold == 6
    assert service.launchConfig.healthCheck.requestLine == "index.html"
    assert service.launchConfig.healthCheck.port == 200

    # activate the service and validate that parameters were set for instance
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    instance_service_map = client \
        .list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + "1")
    assert len(instances) == 1
    container = instances[0]
    assert container.imageUuid == image_uuid
    assert container.command == ['sleep', '42']
    assert len(container.instanceLinks()) == 1
    assert len(container.environment) == 1
    assert len(container.ports()) == 2
    assert len(container.dataVolumes) == 1
    assert set(container.dataVolumesFrom) == set([container1.id])
    assert container.capAdd == caps
    assert container.capDrop == caps
    assert container.dns == dns
    assert container.dnsSearch == dns
    assert container.privileged is True
    assert container.domainName == "rancher.io"
    assert container.memory == 8000000
    assert container.stdinOpen is True
    assert container.tty is True
    assert container.entryPoint == ["/bin/sh", "-c"]
    assert container.cpuShares == 400
    assert container.restartPolicy == restart_policy
    assert container.directory == "/"
    assert container.hostname == "test"
    assert container.user == "test"
    assert container.state == "running"
    assert container.cpuSet == "2"
    assert container.requestedHostId == host.id
    assert container.healthState == 'healthy'


def test_activate_services(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    env.activateservices()
    service1 = client.wait_success(service1, 120)
    service2 = client.wait_success(service2, 120)
    assert service1.state == "active"
    assert service2.state == "active"


def _validate_instance_stopped(service, client, env):
    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + "1")
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        client, instance, _resource_is_stopped,
        lambda x: 'State is: ' + x.state)


def _validate_compose_instance_removed(client, service, env, number="1"):
    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + number)
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        client, instance, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _validate_instance_removed(client, name):
    instances = client. \
        list_container(name=name)
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        client, instance, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_deactivate_remove_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(client, service, env, "1")

    # deactivate service
    service = client.wait_success(service.deactivate())
    assert service.state == "inactive"
    _validate_instance_stopped(service, client, env)

    # remove service
    service = client.wait_success(service.remove())
    _validate_compose_instance_removed(client, service, env)


def test_env_deactivate_services(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    # activate services
    env = env.activateservices()
    service1 = client.wait_success(service1, 120)
    service2 = client.wait_success(service2, 120)
    assert service1.state == "active"
    assert service2.state == "active"

    # deactivate services
    env.deactivateservices()
    service1 = client.wait_success(service1)
    service2 = client.wait_success(service2)
    assert service1.state == "inactive"
    assert service2.state == "inactive"
    _validate_instance_stopped(service1, client, env)
    _validate_instance_stopped(service2, client, env)


def test_remove_inactive_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    client.create_service(name=random_str(),
                          environmentId=env.id,
                          launchConfig=launch_config)
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(client, service, env, "1")

    # deactivate service
    service = client.wait_success(service.deactivate())
    assert service.state == "inactive"

    # remove service
    service = client.wait_success(service.remove())
    assert service.state == "removed"
    _validate_compose_instance_removed(client, service, env)


def test_remove_environment(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env = env.activateservices()
    service = client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(client, service, env, "1")

    # deactivate services
    env = env.deactivateservices()
    service = client.wait_success(service)
    assert service.state == "inactive"

    # remove environment
    env = client.wait_success(env.remove())
    assert env.state == "removed"
    wait_for_condition(
        client, service, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_create_duplicated_services(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    service_name = random_str()
    service1 = client.create_service(name=service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    client.wait_success(service1)

    with pytest.raises(ApiError) as e:
        client.create_service(name=service_name,
                              environmentId=env.id,
                              launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'


def test_service_add_remove_service_link(client, super_client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link service2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # remove service link
    service1 = service1.removeservicelink(serviceId=service2.id)
    _validate_remove_service_link(service1, service2, super_client)


def test_link_service_twice(super_client, client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link service2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'serviceId'


def test_links_after_service_remove(super_client, client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # link servic2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # link service1 to service2
    service2 = service2.addservicelink(serviceId=service1.id)
    _validate_add_service_link(service2, service1, super_client)

    # remove service1
    service1 = client.wait_success(service1.remove())

    _validate_remove_service_link(service1, service2, super_client)

    _validate_remove_service_link(service2, service1, super_client)


def test_link_volumes(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)
    service1 = client.wait_success(service1.activate(), 120)
    container1 = _validate_compose_instance_start(client,
                                                  service1, env, "1")

    external_container = client.create_container(
        imageUuid=image_uuid,
        requestedHostId=container1.hosts()[0].id)
    external_container = client.wait_success(external_container)

    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFrom": [external_container.id],
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service2 = client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id])

    service2 = client.wait_success(service2)
    service2 = client.wait_success(service2.activate(), 120)
    container2 = _validate_compose_instance_start(client,
                                                  service2, env, "1")

    # verify that the instance started in service2,
    # got volume of instance of service1
    assert len(container2.dataVolumesFrom) == 2
    assert set(container2.dataVolumesFrom) == set([external_container.id,
                                                   container1.id])


def test_volumes_service_links_scale_one(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service2 = client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id])
    service2 = client.wait_success(service2)

    service3 = client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id, service2.id])
    service3 = client.wait_success(service3)

    service1 = client.wait_success(service1.activate(), 120)
    service2 = client.wait_success(service2, 120)
    service3 = client.wait_success(service3, 120)

    assert service1.state == "active"
    assert service3.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    s1_container = _validate_compose_instance_start(client,
                                                    service1, env, "1")
    s2_container = _validate_compose_instance_start(client,
                                                    service2, env, "1")
    s3_container = _validate_compose_instance_start(client,
                                                    service3, env, "1")

    assert len(s2_container.dataVolumesFrom) == 1
    assert set(s2_container.dataVolumesFrom) == set([s1_container.id])

    assert len(s3_container.dataVolumesFrom) == 2
    assert set(s3_container.dataVolumesFrom) == set([s1_container.id,
                                                     s2_container.id])


def test_volumes_service_links_scale_two(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)

    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service2 = client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id],
                       scale=2)
    service2 = client.wait_success(service2)

    service1 = client.wait_success(service1.activate(), 120)
    service2 = client.wait_success(service2, 120)

    assert service1.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service1, env, "2")
    s21_container = _validate_compose_instance_start(client,
                                                     service2, env, "1")
    s22_container = _validate_compose_instance_start(client,
                                                     service2, env, "2")

    assert len(s22_container.dataVolumesFrom) == 1
    assert len(s21_container.dataVolumesFrom) == 1


def test_remove_active_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(client, service, env, "1")

    # remove service
    service = client.wait_success(service.remove(), 120)
    assert service.state == "removed"
    _validate_compose_instance_removed(client, service, env)


def _wait_until_active_map_count(service, count, client, timeout=30):
    # need this function because agent state changes
    # active->deactivating->removed
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    while len(instance_service_map) != count:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id, state="active")
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be removed.'

    return


def test_remove_environment_w_active_svcs(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env = env.activateservices()
    service = client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(client, service, env, "1")

    # remove environment
    env = client.wait_success(env.remove())
    assert env.state == "removed"
    service = client.wait_success(service)
    _validate_compose_instance_removed(client, service, env)


def _validate_compose_instance_start(client, service, env, number):
    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + number,
                       state="running")
    assert len(instances) == 1
    return instances[0]


def _validate_instance_start(service, client, name):
    instances = client. \
        list_container(name=name)
    assert len(instances) == 1
    return instances[0]


def test_validate_service_scaleup_scaledown(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # scale up the inactive service
    service = client.update(service, scale=3, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "inactive"
    assert service.scale == 3

    # activate services
    env.activateservices()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    _validate_compose_instance_start(client, service, env, "1")
    instance2 = _validate_compose_instance_start(client, service,
                                                 env, "2")
    instance3 = _validate_compose_instance_start(client, service,
                                                 env, "3")

    # stop the instance2
    instance2 = client.wait_success(instance2)
    instance2 = client.wait_success(instance2.stop())
    assert instance2.state == 'stopped'

    # rename the instance 3
    instance3 = client.update(instance3, name='newName')

    # scale up the service
    # instance 2 should get started; env_service_3 name should be utilized
    service = client.update(service, scale=4, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 4

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "2")
    _validate_compose_instance_start(client, service, env, "3")
    _validate_instance_start(service, client, instance3.name)

    # scale down the service
    service = client.update(service, scale=2, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    # validate that only 2 service instance mappings exist
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 2


def test_link_services_from_diff_env(client, context):
    env1 = client.create_environment(name=random_str())
    env1 = client.wait_success(env1)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    env2 = client.create_environment(name=random_str())
    env2 = client.wait_success(env2)
    service2 = client.create_service(name=random_str(),
                                     environmentId=env2.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    # try to link
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'serviceId'


def test_set_service_links(super_client, client, context):
    env1 = client.create_environment(name=random_str())
    env1 = client.wait_success(env1)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    service3 = client.create_service(name=random_str(),
                                     environmentId=env1.id,
                                     launchConfig=launch_config)
    service3 = client.wait_success(service3)

    # set service2, service3 links for service1
    service1 = service1.setservicelinks(serviceIds=[service2.id, service3.id])
    _validate_add_service_link(service1, service2, super_client)
    _validate_add_service_link(service1, service3, super_client)

    # set service2 links for service1
    service1 = service1.setservicelinks(serviceIds=[service2.id])
    _validate_add_service_link(service1, service2, super_client)
    _validate_remove_service_link(service1, service3, super_client)

    # set empty service link set
    service1 = service1.setservicelinks(serviceIds=[])
    _validate_remove_service_link(service1, service2, super_client)
    _validate_remove_service_link(service1, service3, super_client)

    # try to link to the service from diff environment
    env2 = client.create_environment(name=random_str())
    env2 = client.wait_success(env2)

    service4 = client.create_service(name=random_str(),
                                     environmentId=env2.id,
                                     launchConfig=launch_config)
    service4 = client.wait_success(service4)

    with pytest.raises(ApiError) as e:
        service1.setservicelinks(serviceIds=[service4.id])

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'serviceId'


def _instance_remove(instance, client):
    instance = client.wait_success(instance)
    instance = client.wait_success(instance.stop())
    assert instance.state == 'stopped'
    instance = client.wait_success(instance.remove())
    assert instance.state == 'removed'
    return instance


def test_destroy_service_instance(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=3)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service.activate()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance2 = _validate_compose_instance_start(client, service, env, "2")
    instance3 = _validate_compose_instance_start(client, service, env, "3")

    # 1. stop and remove the instance2. Validate the mapping still exist
    instance2 = _instance_remove(instance2, client)

    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance2.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    # 2. deactivate the service
    service.deactivate()
    service = client.wait_success(service, 120)
    assert service.state == "inactive"

    # 3. activate the service. The map should be gone
    service.activate()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    # 4. destroy instance3 and update the service's scale.
    # Validate that instance3 map is gone
    instance3 = _instance_remove(instance3, client)
    service = client.update(service, scale=4, name=service.name)
    service = client.wait_success(service, 120)

    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance3.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    # purge the instance1 w/o changing the service
    # and validate instance1-service map is gone
    instance1 = _instance_remove(instance1, client)
    instance1 = client.wait_success(instance1.purge())
    assert instance1.state == 'purged'
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance1.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_service_rename(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)

    # activate service
    service1.activate()
    service1 = client.wait_success(service1, 120)
    assert service1.state == "active"

    _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service1, env, "2")

    # update name and validate that the service name got
    # updated, all old instances weren't renamed,
    # and the new instance got created with the new name
    new_name = "newname"
    service2 = client.update(service1, scale=3, name=new_name)
    service2 = client.wait_success(service2)
    assert service2.name == new_name
    _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service1, env, "2")
    _validate_compose_instance_start(client, service2, env, "1")


def test_env_rename(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service_1 = client.create_service(name=random_str(),
                                      environmentId=env.id,
                                      launchConfig=launch_config,
                                      scale=2)
    service_1 = client.wait_success(service_1)

    service_2 = client.create_service(name=random_str(),
                                      environmentId=env.id,
                                      launchConfig=launch_config,
                                      scale=1)
    service_2 = client.wait_success(service_2)

    # activate services
    env = env.activateservices()
    service_1 = client.wait_success(service_1, 120)
    service_2 = client.wait_success(service_2, 120)
    assert service_1.state == "active"
    assert service_2.state == "active"

    _validate_compose_instance_start(client, service_1, env, "1")
    _validate_compose_instance_start(client, service_1, env, "2")
    _validate_compose_instance_start(client, service_2, env, "1")

    # update env name and validate that the
    # env name got updated, but instances have old names
    new_name = "newname"
    env_updated = client.update(env, name=new_name)
    env_updated = client.wait_success(env_updated)
    assert env_updated.name == new_name
    _validate_compose_instance_start(client, service_1, env, "1")
    _validate_compose_instance_start(client, service_1, env, "2")
    _validate_compose_instance_start(client, service_2, env, "1")


def test_validate_scale_down_restore_state(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=3)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env.activateservices()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance2 = _validate_compose_instance_start(client, service, env, "2")
    instance3 = _validate_compose_instance_start(client, service, env, "3")
    # stop the instances 1, 2 and destroy instance 3
    instance1 = client.wait_success(instance1.stop())
    assert instance1.state == 'stopped'
    instance2 = client.wait_success(instance2.stop())
    assert instance2.state == 'stopped'
    instance3 = _instance_remove(instance3, client)
    assert instance3.state == 'removed'

    # scale down the service and validate that:
    # first instance is running
    # second instance is removed
    # third instance is removed
    service = client.update(service, scale=1, name=service.name)
    client.wait_success(service)

    # validate that only one service instance mapping exists
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 1


def test_validate_labels(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service1 with labels defined
    service_name1 = random_str()
    initial_labels1 = {'affinity': "container==B", '!affinity': "container==C"}
    image_uuid = context.image_uuid
    launch_config1 = {"imageUuid": image_uuid, "labels": initial_labels1}

    service1 = client.create_service(name=service_name1,
                                     environmentId=env.id,
                                     launchConfig=launch_config1)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"
    assert service1.launchConfig.labels == initial_labels1

    # create service2 w/o labels defined
    service_name2 = random_str()
    image_uuid = context.image_uuid
    launch_config2 = {"imageUuid": image_uuid}

    service2 = client.create_service(name=service_name2,
                                     environmentId=env.id,
                                     launchConfig=launch_config2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"
    assert "labels" not in service2.launchConfig

    # activate services
    env.activateservices()
    service1 = client.wait_success(service1, 120)
    assert service1.state == "active"
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"

    # check that labels defined in launch config + the internal label, are set
    result_labels_1 = {'affinity': 'container==B', '!affinity': "container==C",
                       'io.rancher.service.name': service_name1,
                       'io.rancher.environment.name': env.name}
    instance1 = _validate_compose_instance_start(client, service1,
                                                 env, "1")
    assert all(item in instance1.labels for item in result_labels_1) is True

    # check that only one internal label is set
    result_labels_2 = {'io.rancher.service.name': service_name2,
                       'io.rancher.environment.name': env.name}
    instance2 = _validate_compose_instance_start(client, service2,
                                                 env, "1")
    assert all(item in instance2.labels for item in result_labels_2) is True


def test_sidekick_services_activate(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    # service3 with a diff sidekick label, and service4 with no label
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service2 = client.wait_success(service2)

    launch_config1 = {"imageUuid": image_uuid}
    service3 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config1)
    service3 = client.wait_success(service3)

    launch_config2 = {"imageUuid": image_uuid,
                      "labels": {'io.rancher.service.sidekick': "random123"}}
    service4 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config2)
    service4 = client.wait_success(service4)

    # activate service1, service 2 should be activated too
    service1 = client.wait_success(service1.activate(), 120)
    assert service1.state == "active"
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"

    # service 3 and 4 should be inactive
    service3 = client.wait_success(service3)
    assert service3.state == "inactive"
    service4 = client.wait_success(service4)
    assert service4.state == "inactive"


def test_sidekick_restart_instances(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config, scale=2)
    service2 = client.wait_success(service2)

    # activate service1, service 2 should be activated too
    service1 = client.wait_success(service1.activate(), 120)
    assert service1.state == "active"
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"

    instance11 = _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service1, env, "2")
    _validate_compose_instance_start(client, service2, env, "1")
    instance22 = _validate_compose_instance_start(client, service2, env, "2")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2

    # stop instance11, destroy instance12 and call update on a service1
    # scale should be restored
    client.wait_success(instance11.stop())
    _instance_remove(instance22, client)
    service1 = client.update(service1, scale=2, name=service1.name)
    service1 = client.wait_success(service1, 120)

    _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service1, env, "2")
    _validate_compose_instance_start(client, service2, env, "1")
    _validate_compose_instance_start(client, service2, env, "2")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2


def test_sidekick_scaleup(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=1)
    service1 = client.wait_success(service1)

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config, scale=1)
    service2 = client.wait_success(service2)

    # activate service1, service 2 should be activated too
    service1 = client.wait_success(service1.activate(), 120)
    assert service1.state == "active"
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"

    _validate_compose_instance_start(client, service1, env, "1")
    _validate_compose_instance_start(client, service2, env, "1")

    # scale up service1, verify that the service 2 was scaled up and updated
    service1 = client.update(service1, scale=2, name=service1.name)
    _wait_compose_instance_start(client, service1, env, "1")
    _wait_compose_instance_start(client, service1, env, "2")
    _wait_compose_instance_start(client, service2, env, "1")
    _wait_compose_instance_start(client, service2, env, "2")

    service1 = client.wait_success(service1, 120)
    assert service1.state == "active"
    assert service1.scale == 2
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"
    assert service2.scale == 2

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2


def test_sidekick_diff_scale(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined,
    # but diff scale - should fail
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)
    assert service1.scale == 2

    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=3)
    service2 = client.wait_success(service2)
    assert service2.scale == 2


def _validate_service_ip_map(client, service, ip, state, timeout=30):
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, ipAddress=ip, state=state)
    while len(instance_service_map) != 1:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id,
                                  ipAddress=ip, state=state)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be in correct state'


def test_external_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    # create service1 as a regular service
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config)
    service1 = client.wait_success(service1)

    # create service 2 as external
    ips = ["72.22.16.5", '192.168.0.10']
    service2 = client.create_externalService(name=random_str(),
                                             environmentId=env.id,
                                             launchConfig=launch_config,
                                             externalIpAddresses=ips)
    service2 = client.wait_success(service2)

    # activate services
    env.activateservices()
    service1 = client.wait_success(service1)
    assert service1.state == 'active'

    service2 = client.wait_success(service2)
    assert service2.state == 'active'
    assert service2.externalIpAddresses == ips
    _validate_service_ip_map(client, service2, "72.22.16.5", "active")
    _validate_service_ip_map(client, service2, "192.168.0.10", "active")

    # deactivate external service
    service2 = client.wait_success(service2.deactivate())
    assert service2.state == "inactive"
    _validate_service_ip_map(client, service2, "72.22.16.5", "removed")
    _validate_service_ip_map(client, service2, "192.168.0.10", "removed")

    # activate external service again
    service2 = client.wait_success(service2.activate())
    assert service2.state == "active"
    _validate_service_ip_map(client, service2, "72.22.16.5", "active")
    _validate_service_ip_map(client, service2, "192.168.0.10", "active")

    # remove external service
    service2 = client.wait_success(service2.remove())
    assert service2.state == "removed"


def test_service_spread_deployment(super_client, new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    super_client.update(host1, {'computeFree': 1000000})
    super_client.update(host2, {'computeFree': 1000000})

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env.activateservices()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    # since both hosts should have equal compute_free, the
    # containers should be spread across the hosts
    instance1 = _validate_compose_instance_start(client,
                                                 service, env, "1")
    instance1_host = instance1.hosts()[0].id

    instance2 = _validate_compose_instance_start(client,
                                                 service, env, "2")
    instance2_host = instance2.hosts()[0].id
    assert instance1_host != instance2_host


def test_global_service(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    # add labels to the hosts
    labels = {'group': 'web'}
    host1 = client.update(host1, labels=labels)
    host2 = client.update(host2, labels=labels)

    # create environment and services
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    image_uuid = new_context.image_uuid
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true',
            'io.rancher.scheduler.affinity:host_label': 'group=web'
        }
    }
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    # 2. verify that the instance was started on host1
    instance1 = _validate_compose_instance_start(client,
                                                 service, env, "1")
    instance1_host = instance1.hosts()[0].id

    # 3. verify that the instance was started on host2
    instance2 = _validate_compose_instance_start(client,
                                                 service, env, "2")
    instance2_host = instance2.hosts()[0].id
    assert instance1_host != instance2_host


def test_global_service_update_label(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    # add labels to the hosts
    labels = {'group': 'web'}
    host1 = client.update(host1, labels=labels)

    # create environment and services
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    image_uuid = new_context.image_uuid
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true',
            'io.rancher.scheduler.affinity:host_label': 'group=web'
        }
    }
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # 1. verify that the service was activated
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    # 2. verify that the instance was started on host1
    instance1 = _validate_compose_instance_start(client,
                                                 service, env, "1")
    instance1_host = instance1.hosts()[0].id
    assert instance1_host == host1.id

    # verify 2nd instance isn't running
    assert len(client.list_container(
        name=env.name + "_" + service.name + "_2")) == 0

    # update host2 with label group=web
    host2 = client.update(host2, labels=labels)

    # wait for 2nd instance to start up
    wait_for(
        lambda: len(client.list_container(
            name=env.name + "_" + service.name + "_2",
            state="running")) > 0
    )
    instance2 = _validate_compose_instance_start(client,
                                                 service, env, "2")

    # confirm 2nd instance is on host2
    instance2_host = instance2.hosts()[0].id
    assert instance2_host == host2.id

    # destroy the instance, reactivate the service and check
    #  both hosts got instances
    _instance_remove(instance1, client)
    service = client.wait_success(service.deactivate())
    assert service.state == "inactive"
    service = client.wait_success(service.activate())
    assert service.state == "active"
    instance1 = _validate_compose_instance_start(client,
                                                 service, env, "1")
    instance2 = _validate_compose_instance_start(client,
                                                 service, env, "2")

    instance1_host = instance1.hosts()[0].id
    assert instance1_host == host1.id or instance1_host == host2.id
    assert instance1.hosts()[0].id != instance2.hosts()[0].id


def test_dns_service(super_client, client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    # create 1 app service, 1 dns service and 2 web services
    # app service would link to dns, and dns to the web services
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    web1 = client.create_service(name=random_str(),
                                 environmentId=env.id,
                                 launchConfig=launch_config)
    web1 = client.wait_success(web1)

    web2 = client.create_service(name=random_str(),
                                 environmentId=env.id,
                                 launchConfig=launch_config)
    web2 = client.wait_success(web2)

    app = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config)
    app = client.wait_success(app)

    dns = client.create_dnsService(name='tata',
                                   environmentId=env.id)
    dns = client.wait_success(dns)

    env.activateservices()
    web1 = client.wait_success(web1, 120)
    web2 = client.wait_success(web2)
    app = client.wait_success(app)
    dns = client.wait_success(dns)
    assert web1.state == 'active'
    assert web2.state == 'active'
    assert app.state == 'active'
    assert dns.state == 'active'

    dns = app.addservicelink(serviceId=web1.id)
    _validate_add_service_link(dns, web1, super_client)

    dns = app.addservicelink(serviceId=web2.id)
    _validate_add_service_link(dns, web2, super_client)

    app = app.addservicelink(serviceId=dns.id)
    _validate_add_service_link(app, dns, super_client)


def test_svc_container_reg_cred_and_image(client, super_client):
    server = 'server{0}.io'.format(random_num())
    registry = client.create_registry(serverAddress=server,
                                      name=random_str())
    registry = client.wait_success(registry)
    reg_cred = client.create_registry_credential(
        registryId=registry.id,
        email='test@rancher.com',
        publicValue='rancher',
        secretValue='rancher')
    registry_credential = client.wait_success(reg_cred)
    name = server + '/rancher/authorized:latest'
    image_uuid = 'docker:' + name
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=1)
    service = client.wait_success(service)
    service.activate()
    service = client.wait_success(service, 120)
    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + "1")
    assert len(instances) == 1
    container = instances[0]
    container = super_client.wait_success(container)
    assert container.registryCredentialId == registry_credential.id
    image = container.image()
    assert image.name == name
    assert image.registryCredentialId == registry_credential.id


def test_network_from_service(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)
    assert service1.launchConfig.networkMode == 'managed'

    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"},
                     "networkMode": "container"}
    service2 = client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       launchConfig=launch_config,
                       networkServiceId=service1.id,
                       scale=2)
    service2 = client.wait_success(service2)
    assert service2.launchConfig.networkMode == 'container'

    service1 = client.wait_success(service1.activate(), 120)
    service2 = client.wait_success(service2, 120)

    assert service1.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    s11_container = _validate_compose_instance_start(client,
                                                     service1, env, "1")
    s12_container = _validate_compose_instance_start(client,
                                                     service1, env, "2")
    s21_container = _validate_compose_instance_start(client,
                                                     service2, env, "1")
    s22_container = _validate_compose_instance_start(client,
                                                     service2, env, "2")

    assert s21_container.networkContainerId is not None
    assert s22_container.networkContainerId is not None
    assert s21_container.networkContainerId != s22_container.networkContainerId
    assert s21_container.networkContainerId in [s11_container.id,
                                                s12_container.id]
    assert s21_container.networkMode == 'container'
    assert s22_container.networkMode == 'container'
    assert s11_container.networkMode == 'managed'
    assert s12_container.networkMode == 'managed'


def _wait_compose_instance_start(client, service, env, number, timeout=30):
    start = time.time()
    instances = client. \
        list_container(name=env.name + "_" + service.name + "_" + number,
                       state="running")
    while len(instances) != 1:
        time.sleep(.5)
        instances = client. \
            list_container(name=env.name + "_" + service.name + "_" + number,
                           state="running")
        if time.time() - start > timeout:
            assert 'Timeout waiting for instance to become running.'


def test_service_affinity_rules(super_client, new_context):
    register_simulated_host(new_context)
    register_simulated_host(new_context)

    client = new_context.client

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = new_context.image_uuid
    name = random_str()
    service_name = "service" + name

    # test anti-affinity
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_ne":
            "io.rancher.service.name=" + service_name
        }
    }

    service = client.create_service(name=service_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=3)
    service = client.wait_success(service)
    assert service.state == "inactive"

    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    # check that all containers are on different hosts
    instances = _get_instance_for_service(super_client, service.id)

    assert len(instances) == 3
    assert instances[0].hosts()[0].id != instances[1].hosts()[0].id
    assert instances[1].hosts()[0].id != instances[2].hosts()[0].id
    assert instances[2].hosts()[0].id != instances[0].hosts()[0].id

    # test soft-affinity
    service_name2 = "service2" + name
    launch_config2 = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_soft":
            "io.rancher.service.name=" + service_name2
        }
    }

    service2 = client.create_service(name=service_name2,
                                     environmentId=env.id,
                                     launchConfig=launch_config2,
                                     scale=3)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    service2 = service2.activate()
    service2 = client.wait_success(service2, 120)
    assert service2.state == "active"

    # check that all containers are on the same host
    instances = _get_instance_for_service(super_client, service2.id)

    assert len(instances) == 3
    assert instances[0].hosts()[0].id == instances[1].hosts()[0].id
    assert instances[1].hosts()[0].id == instances[2].hosts()[0].id
    assert instances[2].hosts()[0].id == instances[0].hosts()[0].id


def _get_instance_for_service(super_client, serviceId):
    instances = []
    instance_service_maps = super_client. \
        list_serviceExposeMap(serviceId=serviceId)
    for mapping in instance_service_maps:
        instances.append(mapping.instance())
    return instances


def _resource_is_stopped(resource):
    return resource.state == 'stopped'


def _resource_is_running(resource):
    return resource.state == 'running'


def _validate_add_service_link(service, consumedService, super_client):
    service_maps = super_client. \
        list_serviceConsumeMap(serviceId=service.id,
                               consumedServiceId=consumedService.id)
    assert len(service_maps) == 1
    service_map = service_maps[0]
    wait_for_condition(
        super_client, service_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_service_link(service, consumedService, super_client):
    service_maps = super_client. \
        list_serviceConsumeMap(serviceId=service.id,
                               consumedServiceId=consumedService.id)
    assert len(service_maps) == 1
    service_map = service_maps[0]
    wait_for_condition(
        super_client, service_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'
