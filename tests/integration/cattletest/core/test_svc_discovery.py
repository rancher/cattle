from common_fixtures import *  # NOQA
from cattle import ApiError
import yaml


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
                     "workingDir": "/",
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
    assert service.launchConfig.workingDir == "/"
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
    assert len(container.environment) == 2
    env_vars = {'TEST_FILE': "", "RANCHER_CONTAINER_NAME": ""}
    assert all(item in container.environment for item in env_vars) is True
    assert len(container.ports) == 2
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
    assert container.workingDir == "/"
    assert container.hostname == "test"
    assert container.user == "test"
    assert container.state == "running"
    assert container.cpuSet == "2"
    assert container.requestedHostId == host.id
    assert container.healthState == 'initializing'


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


def test_service_add_remove_service_link(client, context):
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
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # remove service link
    service1 = service1.removeservicelink(serviceLink=service_link)
    _validate_remove_service_link(service1, service2, client)

    # validate adding link with the name
    service_link = {"serviceId": service2.id, "name": 'myLink'}
    service1 = service1.addservicelink(serviceLink=service_link)
    service_maps = client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id, name='mylink')
    assert len(service_maps) == 1


def test_link_service_twice(client, context):
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
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceLink=service_link)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'serviceId'


def test_links_after_service_remove(client, context):
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
    service_link = {"serviceId": service2.id}
    service1 = service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)

    # link service1 to service2
    service_link = {"serviceId": service1.id}
    service2 = service2.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service2, service1, client)

    # remove service1
    service1 = client.wait_success(service1.remove())

    _validate_remove_service_link(service1, service2, client)

    _validate_remove_service_link(service2, service1, client)


def test_link_volumes(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}

    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)
    service = client.wait_success(service.activate(), 120)
    container1 = _validate_compose_instance_start(client, service, env, "1")
    container2 = _validate_compose_instance_start(client, service, env, "1",
                                                  "secondary")

    assert len(container1.dataVolumesFrom) == 1
    assert set(container1.dataVolumesFrom) == set([container2.id])


def test_volumes_service_links_scale_one(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    sec_lc_1 = {"imageUuid": image_uuid, "name": "secondary1",
                "dataVolumesFromLaunchConfigs": ["primary"]}
    sec_lc_2 = {"imageUuid": image_uuid, "name": "secondary2",
                "dataVolumesFromLaunchConfigs":
                    ["primary", "secondary1"]}
    service = client. \
        create_service(name="primary",
                       environmentId=env.id,
                       launchConfig=launch_config,
                       secondaryLaunchConfigs=[sec_lc_1, sec_lc_2])
    service = client.wait_success(service)

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    # 2. validate instances
    s1_container = _validate_compose_instance_start(client, service, env, "1")
    s2_container = _validate_compose_instance_start(client, service, env,
                                                    "1", "secondary1")
    s3_container = _validate_compose_instance_start(client, service, env,
                                                    "1", "secondary2")

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
                     "dataVolumesFromLaunchConfigs": ["secondary"]}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    # 2. validate instances
    s11_container = _validate_compose_instance_start(client, service, env, "1")
    s12_container = _validate_compose_instance_start(client, service, env, "2")
    _validate_compose_instance_start(client, service, env, "1", "secondary")
    _validate_compose_instance_start(client, service, env, "2", "secondary")

    assert len(s11_container.dataVolumesFrom) == 1
    assert len(s12_container.dataVolumesFrom) == 1
    assert set(s12_container.dataVolumesFrom) != set(
        s11_container.dataVolumesFrom)


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


def _validate_compose_instance_start(client, service, env,
                                     number, launch_config_name=None):
    cn = launch_config_name + "_" if \
        launch_config_name is not None else ""
    name = env.name + "_" + service.name + "_" + cn + number
    instances = client. \
        list_container(name=name,
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
    instance2 = _validate_compose_instance_start(client, service, env, "2")
    instance3 = _validate_compose_instance_start(client, service, env, "3")

    # stop the instance2
    client.wait_success(instance2.stop())
    service = client.wait_success(service)

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

    # try to link - should work
    service_link = {"serviceId": service2.id}
    service1.addservicelink(serviceLink=service_link)
    _validate_add_service_link(service1, service2, client)


def test_set_service_links(client, context):
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
    service_link1 = {"serviceId": service2.id, "name": "link1"}
    service_link2 = {"serviceId": service3.id, "name": "link2"}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_add_service_link(service1, service2, client, "link1")
    _validate_add_service_link(service1, service3, client, "link2")

    # update the link with new name
    service_link1 = {"serviceId": service2.id, "name": "link3"}
    service_link2 = {"serviceId": service3.id, "name": "link4"}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link1, service_link2])
    _validate_remove_service_link(service1, service2, client, "link1")
    _validate_remove_service_link(service1, service3, client, "link2")
    _validate_add_service_link(service1, service2, client, "link3")
    _validate_add_service_link(service1, service3, client, "link4")

    # set service2 links for service1
    service_link = {"serviceId": service2.id}
    service1 = service1. \
        setservicelinks(serviceLinks=[service_link])
    _validate_remove_service_link(service1, service3, client, "link4")

    # set empty service link set
    service1 = service1.setservicelinks(serviceLinks=[])
    _validate_remove_service_link(service1, service2, client, "link3")

    # try to link to the service from diff environment - should work
    env2 = client.create_environment(name=random_str())
    env2 = client.wait_success(env2)

    service4 = client.create_service(name=random_str(),
                                     environmentId=env2.id,
                                     launchConfig=launch_config)
    service4 = client.wait_success(service4)

    service_link = {"serviceId": service4.id}
    service1.setservicelinks(serviceLinks=[service_link])

    env1.remove()
    env2.remove()


def _instance_remove(instance, client):
    instance = client.wait_success(client.delete(instance))
    wait_for_condition(client, instance,
                       lambda x: x.state == 'removed')
    return client.reload(instance)


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

    # 1. stop and remove the instance2. Validate the mapping is gone
    _instance_remove(instance2, client)

    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance2.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        client, instance_service_map[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    service = client.wait_success(service)

    # 2. deactivate the service
    service.deactivate()
    service = client.wait_success(service, 120)
    assert service.state == "inactive"

    # 3. activate the service
    service.activate()
    service = client.wait_success(service, 120)
    assert service.state == "active"

    # 4. destroy instance3 and update the service's scale.
    _instance_remove(instance3, client)
    service = client.wait_success(service)

    service = client.update(service, scale=4, name=service.name)
    service = client.wait_success(service, 120)

    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state='Active')
    assert len(instance_service_map) == 4

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
    client.wait_success(instance1.stop())
    client.wait_success(instance2.stop())
    _instance_remove(instance3, client)

    # wait for reconcile
    service = client.wait_success(service)

    # scale down the service and validate that:
    # first instance is running
    # second instance is removed
    # third instance is removed
    service = client.update(service, scale=1, name=service.name)
    client.wait_success(service, 120)

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
                       'io.rancher.stack.name': env.name,
                       'io.rancher.stack_service.name':
                           env.name + '/' + service_name1}
    instance1 = _validate_compose_instance_start(client, service1, env, "1")
    assert all(item in instance1.labels for item in result_labels_1) is True
    assert len(instance1.environment) == 1
    env_vars = {"RANCHER_CONTAINER_NAME": ""}
    assert all(item in instance1.environment for item in env_vars) is True

    # check that only one internal label is set
    result_labels_2 = {'io.rancher.stack.name': env.name,
                       'io.rancher.stack_service.name':
                           env.name + '/' + service_name2}
    instance2 = _validate_compose_instance_start(client, service2, env, "1")
    assert all(item in instance2.labels for item in result_labels_2) is True


def test_sidekick_destroy_instance(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    # activate service1
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    instance11 = _validate_compose_instance_start(client, service, env, "1")
    instance12 = _validate_compose_instance_start(client,
                                                  service,
                                                  env, "1", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 2

    # destroy secondary instance and wait for the service to reconcile
    _instance_remove(instance11, client)
    service = client.wait_success(service)

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "1", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 2

    # destroy primary instance and wait for the service to reconcile
    _instance_remove(instance12, client)
    service = client.wait_success(service)

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "1", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 2


def test_sidekick_restart_instances(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    # activate service1
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    instance11 = _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "2")
    _validate_compose_instance_start(client, service, env, "1", "secondary")
    instance22 = _validate_compose_instance_start(client, service,
                                                  env, "2", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 4

    # stop instance11, destroy instance12 and call update on a service1
    # scale should be restored
    client.wait_success(instance11.stop())
    _instance_remove(instance22, client)
    service = client.wait_success(service)
    service = client.update(service, scale=2, name=service.name)
    service = client.wait_success(service, 120)

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "2")
    _validate_compose_instance_start(client, service, env, "1", "secondary")
    _validate_compose_instance_start(client, service, env, "2", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 4


def test_sidekick_scaleup(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=1,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    # activate service1, service 2 should be activated too
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "1", "secondary")

    # scale up service1, verify that the service 2 was scaled up and updated
    service = client.update(service, scale=2, name=service.name)
    _wait_compose_instance_start(client, service, env, "1")
    _wait_compose_instance_start(client, service, env, "2")
    _wait_compose_instance_start(client, service, env, "1", "secondary")
    _wait_compose_instance_start(client, service, env, "2", "secondary")

    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 2

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 4


def _validate_service_ip_map(client, service, ip, state, timeout=30):
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, ipAddress=ip, state=state)
    while len(instance_service_map) < 1:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id,
                                  ipAddress=ip, state=state)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be in correct state'


def _validate_service_hostname_map(client, service,
                                   host_name, state, timeout=30):
    start = time.time()
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id,
                              hostname=host_name, state=state)
    while len(instance_service_map) < 1:
        time.sleep(.5)
        instance_service_map = client. \
            list_serviceExposeMap(serviceId=service.id,
                                  hostname=host_name, state=state)
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be in correct state'


def test_external_service_w_ips(client, context):
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

    # add one extra ip address
    ips = ["72.22.16.5", '192.168.0.10', '10.1.1.1']
    service2 = client.update(service2, externalIpAddresses=ips)
    service2 = client.wait_success(service2, 120)
    assert len(service2.externalIpAddresses) == 3
    _validate_service_ip_map(client, service2, "72.22.16.5", "active")
    _validate_service_ip_map(client, service2, "192.168.0.10", "active")
    _validate_service_ip_map(client, service2, "10.1.1.1", "active")

    # remove 2 ips from the list, and add one new
    ips = ["72.22.16.5", '50.255.37.17']
    service2 = client.update(service2, externalIpAddresses=ips)
    service2 = client.wait_success(service2, 120)
    assert len(service2.externalIpAddresses) == 2
    _validate_service_ip_map(client, service2, "72.22.16.5", "active")
    _validate_service_ip_map(client, service2, "192.168.0.10", "removed")
    _validate_service_ip_map(client, service2, "10.1.1.1", "removed")
    _validate_service_ip_map(client, service2, "50.255.37.17", "active")

    # remove external service
    service2 = client.wait_success(service2.remove())
    assert service2.state == "removed"


def test_external_service_w_hostname(client, context):
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
    service2 = client.create_externalService(name=random_str(),
                                             environmentId=env.id,
                                             launchConfig=launch_config,
                                             hostname="a.com")
    service2 = client.wait_success(service2)

    # activate services
    env.activateservices()
    service1 = client.wait_success(service1)
    assert service1.state == 'active'

    service2 = client.wait_success(service2)
    assert service2.state == 'active'
    assert service2.hostname == "a.com"
    _validate_service_hostname_map(client, service2, "a.com", "active")

    # deactivate external service
    service2 = client.wait_success(service2.deactivate())
    assert service2.state == "inactive"
    _validate_service_hostname_map(client, service2, "a.com", "removed")

    # activate external service again
    service2 = client.wait_success(service2.activate())
    assert service2.state == "active"
    _validate_service_hostname_map(client, service2, "a.com", "active")

    # change hostname
    service2 = client.update(service2, hostname="b.com")
    service2 = client.wait_success(service2, 120)
    assert service2.hostname == "b.com"
    _validate_service_hostname_map(client, service2, "b.com", "active")
    _validate_service_hostname_map(client, service2, "a.com", "removed")

    # remove external service
    service2 = client.wait_success(service2.remove())
    assert service2.state == "removed"

    # try to create external service with both hostname externalips
    with pytest.raises(ApiError) as e:
        ips = ["72.22.16.5", '192.168.0.10']
        client.create_externalService(name=random_str(),
                                      environmentId=env.id,
                                      launchConfig=launch_config,
                                      hostname="a.com",
                                      externalIpAddresses=ips)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'


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
    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance1_host = instance1.hosts()[0].id

    instance2 = _validate_compose_instance_start(client, service, env, "2")
    instance2_host = instance2.hosts()[0].id
    assert instance1_host != instance2_host


def test_global_service(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    # add labels to the hosts
    labels = {'group': 'web', 'subgroup': 'Foo'}
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
            'io.rancher.scheduler.affinity:host_label':
                'group=Web,subgroup=foo'
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
    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance1_host = instance1.hosts()[0].id

    # 3. verify that the instance was started on host2
    instance2 = _validate_compose_instance_start(client, service, env, "2")
    instance2_host = instance2.hosts()[0].id
    assert instance1_host != instance2_host
    service.deactivate()


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
    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance1_host = instance1.hosts()[0].id
    assert instance1_host == host1.id

    # verify 2nd instance isn't running
    assert len(client.list_container(
        name=env.name + "_" + service.name + "_2")) == 0

    # update host2 with label group=web
    host2 = client.wait_success(client.update(host2, labels=labels))
    service = client.wait_success(service)

    # wait for 2nd instance to start up
    wait_for(
        lambda: len(client.list_container(
            name=env.name + "_" + service.name + "_2",
            state="running")) > 0
    )
    instance2 = _validate_compose_instance_start(client, service, env, "2")

    # confirm 2nd instance is on host2
    instance2_host = instance2.hosts()[0].id
    assert instance2_host == host2.id

    # destroy the instance, reactivate the service and check
    # both hosts got instances
    _instance_remove(instance1, client)
    service = client.wait_success(service.deactivate(), 120)
    assert service.state == "inactive"
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"
    instance1 = _validate_compose_instance_start(client, service, env, "1")
    instance2 = _validate_compose_instance_start(client, service, env, "2")

    instance1_host = instance1.hosts()[0].id
    assert instance1_host == host1.id or instance1_host == host2.id
    assert instance1.hosts()[0].id != instance2.hosts()[0].id
    service.deactivate()


def test_global_add_host(new_context):
    client = new_context.client
    host1 = new_context.host

    # create environment and services
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    image_uuid = new_context.image_uuid
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.scheduler.global': 'true'
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

    # register new host
    host2 = register_simulated_host(new_context)

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
    service.deactivate()


def test_dns_service(client, context):
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

    service_link = {"serviceId": web1.id}
    dns = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(dns, web1, client)

    service_link = {"serviceId": web2.id}
    dns = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(dns, web2, client)

    service_link = {"serviceId": dns.id}
    app = app.addservicelink(serviceLink=service_link)
    _validate_add_service_link(app, dns, client)


def test_svc_container_reg_cred_and_image(super_client, client):
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
    launch_config = {"imageUuid": image_uuid, "networkMode": 'container',
                     "networkLaunchConfig": "secondary"}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)
    assert len(service.secondaryLaunchConfigs) == 1
    assert service.launchConfig.networkMode == 'container'
    assert service.secondaryLaunchConfigs[0].networkMode == 'managed'

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    # 2. validate instances
    s11_container = _validate_compose_instance_start(client, service, env, "1")
    s12_container = _validate_compose_instance_start(client, service, env, "2")
    s21_container = _validate_compose_instance_start(client, service,
                                                     env, "1", "secondary")
    s22_container = _validate_compose_instance_start(client, service,
                                                     env, "2", "secondary")

    assert s11_container.networkContainerId is not None
    assert s12_container.networkContainerId is not None
    assert s11_container.networkContainerId != s12_container.networkContainerId
    assert s11_container.networkContainerId in [s21_container.id,
                                                s22_container.id]

    assert s11_container.networkMode == 'container'
    assert s12_container.networkMode == 'container'
    assert s21_container.networkMode == 'managed'
    assert s22_container.networkMode == 'managed'


def test_circular_refs(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # test direct circular ref
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}

    secondary_lc = {"imageUuid": image_uuid, "name": "secondary",
                    "dataVolumesFromLaunchConfigs": ['primary']}

    with pytest.raises(ApiError) as e:
        client.create_service(name="primary",
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'

    # test indirect circular ref
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary1']}

    s_lc1 = {"imageUuid": image_uuid, "name": "secondary1",
             "dataVolumesFromLaunchConfigs": ['secondary2']}

    s_lc2 = {"imageUuid": image_uuid, "name": "secondary2",
             "dataVolumesFromLaunchConfigs": ['primary']}

    with pytest.raises(ApiError) as e:
        client.create_service(name="primary",
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[s_lc1, s_lc2])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'


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
                "io.rancher.stack_service.name=" +
                env.name + '/' + service_name
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
                "io.rancher.stack_service.name=" +
                "${stack_name}/${service_name}"
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


def test_affinity_auto_prepend_stack(super_client, new_context):
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
    # only service_name is supplied.
    # env/project/stack should be automatically prepended
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_ne":
                "io.rancher.stack_service.name=" +
                service_name
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


def test_affinity_auto_prepend_stack_other_service(super_client, new_context):
    register_simulated_host(new_context)
    register_simulated_host(new_context)

    client = new_context.client

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = new_context.image_uuid
    service_name1 = "service" + random_str()
    service_name2 = "service" + random_str()

    service1 = client.create_service(name=service_name1,
                                     environmentId=env.id,
                                     launchConfig={
                                         "imageUuid": image_uuid,
                                     },
                                     scale=1)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service1 = client.wait_success(service1.activate(), 120)
    assert service1.state == "active"

    # test anti-affinity
    # only service_name is supplied.
    # env/project/stack should be automatically prepended
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_ne":
                "io.rancher.stack_service.name=" +
                service_name2 + "," + service_name1
        }
    }

    service2 = client.create_service(name=service_name2,
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    service2 = client.wait_success(service2.activate(), 120)
    assert service2.state == "active"

    # check that all containers are on different hosts
    instances = _get_instance_for_service(super_client, service2.id)
    instances.extend(_get_instance_for_service(super_client, service1.id))
    assert len(instances) == 3
    assert instances[0].hosts()[0].id != instances[1].hosts()[0].id
    assert instances[1].hosts()[0].id != instances[2].hosts()[0].id
    assert instances[2].hosts()[0].id != instances[0].hosts()[0].id


def test_anti_affinity_sidekick(new_context):
    register_simulated_host(new_context)

    client = new_context.client

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = new_context.image_uuid
    name = random_str()
    service_name = "service" + name

    # only service name is provided.
    # stack name should be prepended and secondaryLaunchConfig
    # should automatically be appended for the sidekick
    # containers
    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.sidekicks": "secondary",
            "io.rancher.scheduler.affinity:container_label_ne":
                "io.rancher.stack_service.name=" +
                service_name
        }
    }
    secondary_lc = {
        "imageUuid": image_uuid,
        "name": "secondary"
    }

    service = client.create_service(name=service_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    # activate service1
    service.activate()
    service = client.wait_success(service, 120)
    assert service.state == "active"
    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "2")
    _validate_compose_instance_start(client, service, env, "1", "secondary")
    _validate_compose_instance_start(client, service, env, "2", "secondary")

    instance_service_map1 = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map1) == 4


def test_host_delete_reconcile_service(super_client, new_context):
    register_simulated_host(new_context)

    client = new_context.client

    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    image_uuid = new_context.image_uuid
    name = random_str()
    service_name = "service" + name

    launch_config = {
        "imageUuid": image_uuid,
        "labels": {
            "io.rancher.scheduler.affinity:container_label_soft_ne":
                "io.rancher.service.name=" + service_name
        }
    }
    service = client.create_service(name=service_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2)
    service = client.wait_success(service)
    assert service.state == "inactive"

    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    instance1 = _validate_compose_instance_start(client,
                                                 service, env, "1")
    instance2 = _validate_compose_instance_start(client,
                                                 service, env, "2")
    instance1_host = instance1.hosts()[0]
    instance2_host = instance2.hosts()[0]
    assert instance1_host.id != instance2_host.id

    # remove host2
    instance2_host = super_client.wait_success(instance2_host.deactivate())
    instance2_host = super_client.delete(instance2_host)
    super_client.wait_success(instance2_host)

    # check that service is reconciled and instance2 gets recreated
    # on host1.
    # NOTE: This is a little strange, but since we don't stop the instance
    # on the deleted host, that instance will still be in running state,
    # so we'll have 3rd that shows up
    wait_for(
        lambda: len(client.list_container(
            name=env.name + "_" + service.name + "_3",
            state="running")) > 0
    )
    instance2 = client.list_container(
        name=env.name + "_" + service.name + "_3",
        state="running")[0]
    instance2_host = instance2.hosts()[0]
    assert instance1_host.id == instance2_host.id
    service = client.wait_success(service.deactivate(), 120)


def test_service_link_emu_docker_link(super_client, client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    dns = client.create_dns_service(name='dns', environmentId=env.id)

    server = client.create_service(name='server', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)

    server2 = client.create_service(name='server2', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)

    service = client.create_service(name='client', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)

    server3 = client.create_service(name='server3', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)

    server4 = client.create_service(name='server4', launchConfig={
        'imageUuid': context.image_uuid
    }, environmentId=env.id)

    service_link1 = {"serviceId": dns.id, "name": "dns"}
    service_link2 = {"serviceId": server.id, "name": "other"}
    service_link3 = {"serviceId": server2.id, "name": "server2"}
    service_link4 = {"serviceId": server3.id}
    service_link5 = {"serviceId": server4.id, "name": ""}
    service. \
        setservicelinks(serviceLinks=[service_link1,
                                      service_link2, service_link3,
                                      service_link4, service_link5])

    dns = client.wait_success(dns)
    assert dns.state == 'inactive'

    server = client.wait_success(server)
    assert server.state == 'inactive'

    server2 = client.wait_success(server2)
    assert server2.state == 'inactive'

    service = client.wait_success(service)
    assert service.state == 'inactive'

    server3 = client.wait_success(server3)
    assert server3.state == 'inactive'

    server4 = client.wait_success(server4)
    assert server4.state == 'inactive'

    dns = client.wait_success(dns.activate())
    assert dns.state == 'active'

    server = client.wait_success(server.activate())
    assert server.state == 'active'

    server2 = client.wait_success(server2.activate())
    assert server2.state == 'active'

    server3 = client.wait_success(server3.activate())
    assert server3.state == 'active'

    server4 = client.wait_success(server4.activate())
    assert server4.state == 'active'

    service = client.wait_success(service.activate())
    assert service.state == 'active'

    instance = find_one(service.instances)
    instance = super_client.reload(instance)

    links = instance.instanceLinks()

    assert len(links) == 4

    for link in links:
        map = link.serviceConsumeMap()
        assert map.consumedServiceId in {server.id, server2.id,
                                         server3.id, server4.id}
        assert link.instanceId is not None
        expose_map = link.targetInstance().serviceExposeMaps()[0]
        if map.consumedServiceId == server.id:
            assert link.linkName == 'other'
            assert expose_map.serviceId == server.id
        elif map.consumedServiceId == server2.id:
            assert link.linkName == 'server2'
            assert expose_map.serviceId == server2.id
        elif map.consumedServiceId == server3.id:
            assert link.linkName == 'server3'
            assert expose_map.serviceId == server3.id
        elif map.consumedServiceId == server4.id:
            assert link.linkName == 'server4'
            assert expose_map.serviceId == server4.id


def test_export_config(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # create service with cpuset
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, "cpuSet": "0,1"}
    service = client. \
        create_service(name="web",
                       environmentId=env.id,
                       launchConfig=launch_config)

    service = client.wait_success(service)

    compose_config = env.exportconfig()
    assert compose_config is not None
    document = yaml.load(compose_config.dockerComposeConfig)
    assert document[service.name]['cpuset'] == "0,1"


def test_validate_image(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    # 1. invalide image in primary config
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": "ubuntu:14:04"}

    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'imageUuid'

    # 2. invalide image in secondary config
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    secondary_lc = {"imageUuid": "ubuntu:14:04", "name": "secondary"}

    with pytest.raises(ApiError) as e:
        client.create_service(name=random_str(),
                              environmentId=env.id,
                              launchConfig=launch_config,
                              secondaryLaunchConfigs=[secondary_lc])
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'imageUuid'


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


def _validate_add_service_link(service,
                               consumedService, client, link_name=None):
    if link_name is None:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id)
    else:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id,
                                   name=link_name)

    assert len(service_maps) == 1
    if link_name is not None:
        assert service_maps[0].name is not None

    service_map = service_maps[0]
    wait_for_condition(
        client, service_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _validate_remove_service_link(service,
                                  consumedService, client, link_name=None):
    if link_name is None:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id)
    else:
        service_maps = client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumedService.id,
                                   name=link_name)

    assert len(service_maps) == 1

    service_map = service_maps[0]
    wait_for_condition(
        client, service_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'
