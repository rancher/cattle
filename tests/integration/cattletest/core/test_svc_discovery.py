from common_fixtures import *  # NOQA
import yaml
from netaddr import IPNetwork, IPAddress


def _create_stack(client):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def _create_stack_long_name(client, lname):
    env = client.create_environment(name=lname)
    env = client.wait_success(env)
    assert env.state == "active"
    return env


def create_env_and_svc(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    return service, env


def test_mix_cased_labels(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {
        "imageUuid": image_uuid,
        'labels': {
            'aAa': 'AaA',
        }}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    assert launch_config['labels'] == service.launchConfig.labels
    service = client.wait_success(service)
    assert service.state == "inactive"
    service = client.wait_success(service.activate())
    assert launch_config['labels'] == service.launchConfig.labels

    instance = find_one(_get_instance_for_service, client, service.id)
    for k, v in service.launchConfig.labels.items():
        assert instance.labels[k] == v


def test_update_env_service(client, context):
    service, env = create_env_and_svc(client, context)
    new_env_name = env.name + '1'
    new_name = service.name + '1'
    service.name = new_name
    service.scale = None
    service = client.update(service, service)
    assert service.name == new_name

    env.name = new_env_name
    env = client.update(env, env)
    assert env.name == new_env_name


def test_env_set_outputs(client, context):
    service, env = create_env_and_svc(client, context)
    assert env.outputs is None

    env = env.addoutputs(outputs={
        'foo': 'bar',
        'foo2': 'bar2',
    })

    assert env.outputs == {'foo': 'bar', 'foo2': 'bar2'}
    env = client.reload(env)
    assert env.outputs == {'foo': 'bar', 'foo2': 'bar2'}
    assert env.state == 'active'

    env = env.addoutputs(outputs={
        'foo3': 'bar3',
    })
    assert env.outputs == {'foo': 'bar', 'foo2': 'bar2', 'foo3': 'bar3'}


def test_activate_single_service(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    host = context.host
    container1 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True)
    container1 = client.wait_success(container1)

    container2 = client.create_container(imageUuid=image_uuid,
                                         startOnCreate=True)
    container2 = client.wait_success(container2)

    caps = ["SYS_MODULE"]

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
                     "workingDir": "/",
                     "hostname": "test",
                     "user": "test",
                     "instanceLinks": {
                         'container2_link':
                             container2.id},
                     "requestedHostId": host.id,
                     "healthCheck": health_check}

    metadata = {"bar": {"foo": [{"id": 0}]}}
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    metadata=metadata)
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
    assert service.metadata == metadata
    assert service.launchConfig.version == '0'

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
    assert container.workingDir == "/"
    assert container.hostname == "test"
    assert container.user == "test"
    assert container.state == "running"
    assert container.cpuSet == "2"
    assert container.requestedHostId == host.id
    assert container.healthState == 'initializing'
    assert container.deploymentUnitUuid is not None
    assert container.version == '0'


def test_activate_services(client, context):
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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


def test_link_volumes(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}

    labels = {"io.rancher.container.start_once": "true"}
    secondary_lc = {"imageUuid": image_uuid,
                    "name": "secondary", "labels": labels}

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

    container2 = client.wait_success(container2.stop())
    client.wait_success(service)
    assert container2.state == 'stopped'


def test_volumes_service_links_scale_one(client, context):
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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

    instance11 = _validate_compose_instance_start(client, service, env, "1")
    instance21 = _validate_compose_instance_start(client, service, env, "2")
    instance31 = _validate_compose_instance_start(client, service, env, "3")

    assert instance31.createIndex > instance21.createIndex
    assert instance21.createIndex > instance11.createIndex

    # stop the instance2
    client.wait_success(instance21.stop())
    service = client.wait_success(service)

    # rename the instance 3
    instance32 = client.update(instance31, name='newName')

    # scale up the service
    # instance 2 should get started; env_service_3 name should be utilized
    service = client.update(service, scale=4, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 4

    instance12 = _validate_compose_instance_start(client, service, env, "1")
    instance22 = _validate_compose_instance_start(client, service, env, "2")
    instance32 = _validate_instance_start(service, client, instance32.name)
    instance41 = _validate_compose_instance_start(client, service, env, "3")

    assert instance41.createIndex > instance32.createIndex
    assert instance32.createIndex > instance22.createIndex
    assert instance22.createIndex > instance12.createIndex

    # scale down the service
    service = client.update(service, scale=0, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    # validate that only 2 service instance mappings exist
    instance_service_map = client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 0

    # scale up service again, and validate
    # that the new instance got unique index
    service = client.update(service, scale=4, name=service.name)
    service = client.wait_success(service, 120)
    instance42 = _validate_compose_instance_start(client, service, env, "4")
    assert instance42.createIndex > instance41.createIndex
    assert service.createIndex == instance42.createIndex


def _instance_remove(instance, client):
    instance = client.wait_success(client.delete(instance))
    wait_for_condition(client, instance,
                       lambda x: x.state == 'removed')
    return client.reload(instance)


def test_destroy_service_instance(client, context):
    env = _create_stack(client)

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
    _validate_service_instance_map_count(client, service, "active", 4)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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
    env = _create_stack(client)

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

    # check that only one internal label is set
    result_labels_2 = {'io.rancher.stack.name': env.name,
                       'io.rancher.stack_service.name':
                           env.name + '/' + service_name2}
    instance2 = _validate_compose_instance_start(client, service2, env, "1")
    assert all(item in instance2.labels for item in result_labels_2) is True


def test_sidekick_destroy_instance(client, context):
    env = _create_stack(client)

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
    _validate_service_instance_map_count(client, service, "active", 2)

    instance11 = _validate_compose_instance_start(client, service, env, "1")
    instance12 = _validate_compose_instance_start(client,
                                                  service,
                                                  env, "1", "secondary")

    # destroy primary instance and wait for the service to reconcile
    _instance_remove(instance11, client)
    service = client.wait_success(service)
    _validate_service_instance_map_count(client, service, "active", 2)
    instance11 = _validate_compose_instance_start(client, service, env, "1")

    # validate that the secondary instance is still up and running
    instance12 = client.reload(instance12)
    assert instance12.state == 'running'

    # destroy secondary instance and wait for the service to reconcile
    _instance_remove(instance12, client)
    service = client.wait_success(service)
    _validate_service_instance_map_count(client, service, "active", 2)

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "1", "secondary")

    # validate that the primary instance was recreated
    instance11 = client.reload(instance11)
    assert instance11.state == 'removed'


def test_sidekick_restart_instances(client, context):
    env = _create_stack(client)
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

    _wait_until_active_map_count(service, 4, client, timeout=30)

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

    _wait_until_active_map_count(service, 4, client, timeout=30)


def test_sidekick_scaleup(client, context):
    env = _create_stack(client)

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


def _validate_service_instance_map_count(client, service,
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
    env = _create_stack(client)
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
    env = _create_stack(client)
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


def test_service_spread_deployment(super_client, new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    super_client.update(host1, {'computeFree': 1000000})
    super_client.update(host2, {'computeFree': 1000000})

    env = _create_stack(client)
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
    env = _create_stack(client)
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
    env = _create_stack(client)
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
    env = _create_stack(client)
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
    env = _create_stack(client)
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
    env = _create_stack(client)

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

    env = _create_stack(client)

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

    env = _create_stack(client)

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

    env = _create_stack(client)

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

    env = _create_stack(client)

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

    env = _create_stack(client)

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


def test_export_config(client, context):
    env = _create_stack(client)

    # test:
    # cpuCet
    # global vs scale
    image_uuid = context.image_uuid
    labels = {'io.rancher.scheduler.global': 'true'}
    metadata = {"$bar": {"metadata": [{"$id$$foo$bar$$": "${HOSTNAME}"}]}}
    restart_policy = {"maximumRetryCount": 2, "name": "on-failure"}
    launch_config = {"imageUuid": image_uuid,
                     "cpuSet": "0,1", "labels": labels,
                     "restartPolicy": restart_policy}
    service = client. \
        create_service(name="web",
                       environmentId=env.id,
                       launchConfig=launch_config,
                       metadata=metadata)

    service = client.wait_success(service)

    compose_config = env.exportconfig()
    assert compose_config is not None
    docker_yml = yaml.load(compose_config.dockerComposeConfig)
    assert docker_yml[service.name]['cpuset'] == "0,1"
    assert docker_yml[service.name]['labels'] == labels
    assert "restart" not in docker_yml[service.name]

    rancher_yml = yaml.load(compose_config.rancherComposeConfig)
    assert 'scale' not in rancher_yml[service.name]
    updated = {"$$id$$$$foo$$bar$$$$": "$${HOSTNAME}"}
    metadata = {"$$bar": {"metadata": [updated]}}
    assert rancher_yml[service.name]['metadata'] is not None
    assert rancher_yml[service.name]['metadata'] == metadata


def test_validate_create_only_containers(client, context):
    env = _create_stack(client)

    labels = {"io.rancher.container.start_once": "true"}
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, "labels": labels}

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

    instance1 = _wait_for_compose_instance_start(client, service, env, "1")
    _wait_for_compose_instance_start(client, service, env, "2")
    instance3 = _wait_for_compose_instance_start(client, service, env, "3")
    # stop instance1 and destroy instance 3
    client.wait_success(instance1.stop())
    _instance_remove(instance3, client)

    # wait for reconcile
    _wait_until_active_map_count(service, 3, client, timeout=30)
    service = client.wait_success(service)
    assert service.state == "active"

    # validate that instance1 remains in stopped state,
    # and instance 3 was recreated
    instance1 = client.reload(instance1)
    assert instance1.state == 'stopped'
    _wait_for_compose_instance_start(client, service, env, "3")
    # check that the service never went to a updating state, and remains active
    updated = True
    try:
        wait_for_condition(
            client, service, service.state == 'updating-active',
            lambda x: 'State is: ' + x.state, 5)
    except:
        updated = False

    assert updated is False

    # destroy instance from stopped state, and validate it was recreated
    _instance_remove(instance1, client)
    _wait_until_active_map_count(service, 3, client, timeout=30)
    service = client.wait_success(service)
    assert service.state == "active"
    _wait_for_compose_instance_start(client, service, env, "1")


def test_sidekick_destroy_instance_indirect_ref(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFromLaunchConfigs": ['secondary']}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary",
                    "dataVolumesFromLaunchConfigs": ['secondary1']}
    secondary_lc1 = {"imageUuid": image_uuid, "name": "secondary1"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    secondaryLaunchConfigs=[secondary_lc,
                                                            secondary_lc1])
    service = client.wait_success(service)

    # activate service
    service = client.wait_success(service.activate(), 120)
    assert service.state == "active"

    instance11 = _validate_compose_instance_start(client, service, env, "1")
    instance12 = _validate_compose_instance_start(client,
                                                  service,
                                                  env, "1", "secondary")

    instance13 = _validate_compose_instance_start(client,
                                                  service,
                                                  env, "1", "secondary1")

    _wait_until_active_map_count(service, 3, client, timeout=30)

    # destroy secondary1 instance and wait for the service to reconcile
    _instance_remove(instance13, client)
    service = client.wait_success(service)

    _validate_compose_instance_start(client, service, env, "1")
    _validate_compose_instance_start(client, service, env, "1", "secondary")
    _validate_compose_instance_start(client, service, env, "1", "secondary1")

    _wait_until_active_map_count(service, 3, client, timeout=30)
    # validate that the primary and secondary instances got recreated
    instance11 = client.reload(instance11)
    assert instance11.state == 'removed'
    instance12 = client.reload(instance12)
    assert instance12.state == 'removed'


def test_validate_hostname_override(client, context):
    # create environment and services
    env = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config1 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.container.hostname_override': 'container_name'
        }
    }
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config1)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service1 = client.wait_success(service1.activate())
    assert service1.state == "active"
    instance1 = _validate_compose_instance_start(client, service1, env, "1")

    # validate the host was overriden with instancename
    assert instance1.hostname == instance1.name

    # use case 2 - validate that even passed hostname gets overriden
    launch_config2 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.container.hostname_override': 'container_name',
            "hostname": "test"
        }
    }
    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    service2 = client.wait_success(service2.activate())
    assert service2.state == "active"
    instance2 = _validate_compose_instance_start(client, service2, env, "1")

    # validate the host was overriden with instancename
    assert instance2.hostname == instance2.name


def test_validate_long_hostname_override(client, context):
    # create environment and services
    env = _create_stack_long_name(client, "MyLongerStackNameCausingIssue")
    image_uuid = context.image_uuid
    launch_config1 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.container.hostname_override': 'container_name'
        }
    }
    first_service_name = "MyServiceNameLongerThanDNSPrefixLengthAllowed"
    service1 = client.create_service(name=first_service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config1)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service1 = client.wait_success(service1.activate())
    assert service1.state == "active"
    instance1 = _validate_compose_instance_start(client, service1, env, "1")

    # validate the host was overriden with truncated
    # instancename - length should be 64
    trunc_name = "MyLongerStackNameCausingIssue_" \
                 "MyServiceNameLongerThanDNSPrefix_1"
    assert instance1.hostname == trunc_name

    # use case 2 - validate that even passed hostname
    # gets overriden by the truncated instancename
    launch_config2 = {
        "imageUuid": image_uuid,
        "labels": {
            'io.rancher.container.hostname_override': 'container_name',
            "hostname": "test"
        }
    }
    second_service_name = "SecondServiceNameLongerThanDNSPrefixLengthAllowed"
    service2 = client.create_service(name=second_service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    service2 = client.wait_success(service2.activate())
    assert service2.state == "active"
    instance2 = _validate_compose_instance_start(client, service2, env, "1")

    # validate the host was overriden with instancename
    trunc_name2 = "MyLongerStackNameCausingIssue_" \
                  "SecondServiceNameLongerThanDNSPr_1"
    assert instance2.hostname == trunc_name2


def test_validate_long_hostname_with_domainname_override(client, context):
    # create environment and services
    env = _create_stack_long_name(client, "MySecondStackNameCausingIssue")
    image_uuid = context.image_uuid
    launch_config1 = {
        "imageUuid": image_uuid,
        "domainName": "rancher.io",
        "labels": {
            'io.rancher.container.hostname_override': 'container_name'
        }
    }
    first_service_name = "MyServiceNameLongerThanDNSPrefixLength" \
                         "AllowedMyServiceNameLonge"
    service1 = client.create_service(name=first_service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config1)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"

    service1 = client.wait_success(service1.activate())
    assert service1.state == "active"
    instance1 = _validate_compose_instance_start(client, service1, env, "1")

    # validate the host was overriden with truncated
    # instancename - length should be 64
    trunc_name = "MySecondStackNameCausingIssue_" \
                 "MyServiceNameLongerTh_1"
    assert instance1.hostname == trunc_name

    # use case 2 - validate that even passed hostname
    # gets overriden by the truncated instancename
    launch_config2 = {
        "imageUuid": image_uuid,
        "domainName": "rancher.io",
        "labels": {
            'io.rancher.container.hostname_override': 'container_name',
            "hostname": "test"
        }
    }
    second_service_name = "SecondServiceNameLongerThanDNSPrefixLengthAllowed"
    service2 = client.create_service(name=second_service_name,
                                     environmentId=env.id,
                                     launchConfig=launch_config2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"

    service2 = client.wait_success(service2.activate())
    assert service2.state == "active"
    instance2 = _validate_compose_instance_start(client, service2, env, "1")

    # validate the host was overriden with instancename
    trunc_name2 = "MySecondStackNameCausingIssue_" \
                  "SecondServiceNameLong_1"
    assert instance2.hostname == trunc_name2


def test_vip_service(client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    init_labels = {'io.rancher.network.services': "vipService"}
    launch_config = {"imageUuid": image_uuid, "labels": init_labels}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config)
    service = client.wait_success(service)
    assert service.state == "inactive"
    assert service.vip is not None
    assert IPAddress(service.vip) in IPNetwork("169.254.64.0/18")

    service = client.wait_success(service.activate())
    assert service.state == "active"
    instance = _validate_compose_instance_start(client, service, env, "1")
    assert all(item in instance.labels for item in init_labels) is True

    # verify that instance was registered as a provider
    account_id = context.project.id
    networks = client.list_network(kind="hostOnlyNetwork",
                                   accountId=account_id)
    assert len(networks) == 1
    nsps = super_client(None).reload(networks[0]).networkServiceProviders()
    assert len(nsps) == 2
    for nsp in nsps:
        if nsp.kind == "externalProvider":
            vip_nsp = nsp
    assert vip_nsp is not None

    vip_nsp_maps = vip_nsp.networkServiceProviderInstanceMaps()
    assert len(vip_nsp_maps) == 1
    assert vip_nsp_maps[0].instanceId == instance.id

    # delete the service and verify that the instance is no longer a provider
    service = client.wait_success(service.remove())
    _validate_compose_instance_removed(client, service, env)
    vip_nsp_maps = vip_nsp.networkServiceProviderInstanceMaps()
    assert len(vip_nsp_maps) == 1
    assert vip_nsp_maps[0].state in ('removing', 'removed', 'purged')


def test_vip_requested_ip(client, context):
    env = _create_stack(client)
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    vip = "169.254.65.30"
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    vip=vip)
    service = client.wait_success(service)
    assert service.state == "inactive"
    assert service.vip is not None
    assert service.vip == vip


def test_validate_scaledown_updating(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=3)
    service = client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    env.activateservices()
    service = client.wait_success(service)
    assert service.state == "active"

    # change scale two times in a row
    service = client.update(service, scale=10, name=service.name)
    service = client.update(service, scale=1, name=service.name)
    service = client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 1
    _wait_until_active_map_count(service, 1, client, timeout=30)


def test_stop_network_from_container(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, "networkMode": 'container',
                     "networkLaunchConfig": "secondary"}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=1,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)
    assert len(service.secondaryLaunchConfigs) == 1
    assert service.launchConfig.networkMode == 'container'
    assert service.secondaryLaunchConfigs[0].networkMode == 'managed'

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    s11_container = _validate_compose_instance_start(client, service, env, "1")
    s21_container = _validate_compose_instance_start(client, service,
                                                     env, "1", "secondary")
    s11_container = super_client.reload(s11_container)
    init_start_count = s11_container.startCount
    assert init_start_count is not None

    assert s11_container.networkContainerId is not None
    assert s11_container.networkContainerId == s21_container.id

    # stop s21 container, and validate s11 was restarted as well
    s21_container = s21_container.stop()
    client.wait_success(s21_container)

    wait_for(
        lambda:
        super_client.reload(s11_container).startCount > init_start_count
    )

    # restart s21 container, and validate s11 was restarted as well
    init_start_count = super_client.reload(s11_container).startCount
    s21_container = client.reload(s21_container).restart()
    client.wait_success(s21_container)
    wait_for(
        lambda:
        super_client.reload(s11_container).startCount > init_start_count
    )
    init_start_count = super_client.reload(s11_container).startCount


def test_remove_network_from_container(client, context, super_client):
    env = _create_stack(client)
    svc_name = random_str()
    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid, "networkMode": 'container'}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary",
                    "networkLaunchConfig": svc_name}

    service = client.create_service(name=svc_name,
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=1,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)
    assert len(service.secondaryLaunchConfigs) == 1
    assert service.launchConfig.networkMode == 'container'
    assert service.secondaryLaunchConfigs[0].networkMode == 'managed'

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    s11_container = _validate_compose_instance_start(client, service, env, "1")
    s21_container = _validate_compose_instance_start(client, service,
                                                     env, "1", "secondary")
    s11_container = super_client.reload(s11_container)
    init_start_count = s11_container.startCount
    assert init_start_count is not None

    assert s21_container.networkContainerId is not None
    assert s21_container.networkContainerId == s11_container.id

    # remove s11 container, and validate s21 was removed as well
    _instance_remove(s11_container, client)
    wait_for_condition(
        client, s21_container, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    service = client.wait_success(service)
    _wait_until_active_map_count(service, 2, client, timeout=30)


def test_metadata(client, context, super_client):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    metadata = {"bar": {"people": [{"id": 0}]}}
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    metadata=metadata)
    service = client.wait_success(service)
    assert service.metadata == metadata

    metadata = {"bar1": {"foo1": [{"id": 0}]}}
    service = client.update(service, metadata=metadata)
    assert service.metadata == metadata


def test_env_external_id(client):
    env = client.create_environment(name='env-' + random_str(),
                                    externalId='something')
    assert env.externalId == 'something'


def test_sidekick_labels_merge(new_context):
    client = new_context.client
    host1 = register_simulated_host(new_context)
    labels = {'group': 'web', 'subgroup': 'Foo'}
    client.update(host1, labels=labels)

    env = _create_stack(client)
    image_uuid = new_context.image_uuid
    labels = {'foo': "bar"}
    affinity_labels = {'io.rancher.scheduler.affinity:host_label':
                       'group=Web,subgroup=foo'}
    labels.update(affinity_labels)
    launch_config = {"imageUuid": image_uuid, "labels": labels}

    secondary_labels = {'bar': "foo"}
    secondary_labels.update(affinity_labels)
    secondary_lc = {"imageUuid": image_uuid,
                    "name": "secondary", "labels": secondary_labels}

    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)
    service = client.wait_success(service.activate(), 120)
    primary = _validate_compose_instance_start(client, service, env, "1")
    secondary = _validate_compose_instance_start(client, service, env, "1",
                                                 "secondary")

    assert all(item in primary.labels for item in labels) is True
    assert all(item in secondary.labels for item in secondary_labels) is True
    assert all(item in primary.labels for item in secondary_labels) is False
    assert all(item in secondary.labels for item in labels) is False
    assert all(item in primary.labels for item in affinity_labels) is True
    assert all(item in secondary.labels for item in affinity_labels) is True


def test_service_restart(client, context):
    env = _create_stack(client)

    image_uuid = context.image_uuid
    launch_config = {"imageUuid": image_uuid}
    secondary_lc = {"imageUuid": image_uuid, "name": "secondary"}
    service = client.create_service(name=random_str(),
                                    environmentId=env.id,
                                    launchConfig=launch_config,
                                    scale=2,
                                    secondaryLaunchConfigs=[secondary_lc])
    service = client.wait_success(service)

    service = client.wait_success(service.activate(), 120)

    assert service.state == "active"

    # get initial start count for all the instances
    instances = []
    for exposeMap in service.serviceExposeMaps():
        instances.append(client.reload(exposeMap.instance()))

    # restart service
    service = client.\
        wait_success(service.restart(rollingRestartStrategy={}), 120)
    assert service.state == 'active'

    for instance in instances:
        old = instance.startCount
        new = client.reload(instance).startCount
        assert new > old


def _validate_endpoint(endpoints, public_port, host, service):
    host_ip = host.ipAddresses()[0].address
    found = False
    for endpoint in endpoints:
        if host_ip == endpoint.ipAddress:
            if endpoint.port == public_port \
                    and endpoint.hostId == host.id \
                    and endpoint.serviceId == service.id \
                    and endpoint.instanceId is not None:
                found = True
                break
    assert found is True, "Cant find endpoint for " \
                          + host_ip + ":" + str(public_port)


def test_public_endpoints(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    env = _create_stack(client)
    hosts = [host1, host2]

    port1 = 5555
    port2 = 6666

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, "ports": [str(port1) + ':6666']}
    service1 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service1 = client.wait_success(service1)
    assert service1.state == "inactive"
    service1 = client.wait_success(service1.activate(), 120)
    assert service1.state == "active"
    launch_config = {"imageUuid": image_uuid, "ports": [str(port2) + ':6666']}
    service2 = client.create_service(name=random_str(),
                                     environmentId=env.id,
                                     launchConfig=launch_config,
                                     scale=2)
    service2 = client.wait_success(service2)
    assert service2.state == "inactive"
    service2 = client.wait_success(service2.activate(), 120)
    assert service2.state == "active"

    wait_for(
        lambda: client.reload(service1).publicEndpoints is not None and len(
            client.reload(service1).publicEndpoints) == 2)
    endpoints = client.reload(service1).publicEndpoints
    for host in hosts:
        _validate_endpoint(endpoints, port1, host, service1)

    wait_for(
        lambda: client.reload(service2).publicEndpoints is not None and len(
            client.reload(service2).publicEndpoints) == 2)
    endpoints = client.reload(service2).publicEndpoints
    for host in hosts:
        _validate_endpoint(endpoints, port2, host, service2)

    wait_for(lambda: client.reload(host1).publicEndpoints is not None and len(
        client.reload(host1).publicEndpoints) == 2)
    endpoints = client.reload(host1).publicEndpoints
    _validate_endpoint(endpoints, port1, host1, service1)
    _validate_endpoint(endpoints, port2, host1, service2)

    wait_for(lambda: client.reload(host2).publicEndpoints is not None and len(
        client.reload(host2).publicEndpoints) == 2)
    endpoints = client.reload(host2).publicEndpoints
    _validate_endpoint(endpoints, port1, host2, service1)
    _validate_endpoint(endpoints, port2, host2, service2)

    # deactivate service1
    service1 = client.wait_success(service1.deactivate())
    assert service1.state == "inactive"

    wait_for(lambda: len(client.reload(service1).publicEndpoints) == 0)
    wait_for(lambda: len(client.reload(service2).publicEndpoints) == 2)
    wait_for(lambda: len(client.reload(host1).publicEndpoints) == 1)
    wait_for(lambda: len(client.reload(host2).publicEndpoints) == 1)


def test_random_ports(new_context):
    client = new_context.client
    new_context.host
    register_simulated_host(new_context)
    env = _create_stack(client)

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, "ports": ['6666', '7775']}
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config,
                                scale=2)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc = client.wait_success(svc.activate())
    assert svc.state == "active"
    c1 = _wait_for_compose_instance_start(client, svc, env, "1")
    c2 = _wait_for_compose_instance_start(client, svc, env, "1")
    port11 = c1.ports_link()[0]
    port12 = c1.ports_link()[1]
    port21 = c2.ports_link()[0]
    port22 = c2.ports_link()[1]
    assert port11.publicPort is not None
    assert port12.publicPort is not None
    assert port21.publicPort is not None
    assert port22.publicPort is not None
    assert port11.publicPort != port12.publicPort
    assert port11.publicPort == port21.publicPort
    assert 49153 <= port11.publicPort <= 65535
    assert 49153 <= port12.publicPort <= 65535
    assert 49153 <= port21.publicPort <= 65535
    assert 49153 <= port22.publicPort <= 65535


def test_random_ports_sidekicks(new_context):
    client = new_context.client
    new_context.host
    register_simulated_host(new_context)
    env = _create_stack(client)

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, "ports": ['6666', '7775']}
    secondary_lc = {"imageUuid": image_uuid,
                    "name": "secondary", "ports": ['6666']}

    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary_lc])
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc = client.wait_success(svc.activate())
    assert svc.state == "active"
    c1 = _wait_for_compose_instance_start(client, svc, env, "1")
    c2 = _validate_compose_instance_start(client, svc,
                                          env, "1", "secondary")

    port1 = c1.ports_link()[0]
    port2 = c2.ports_link()[0]

    assert 49153 <= port1.publicPort <= 65535
    assert 49153 <= port2.publicPort <= 65535


def test_random_ports_static_port(new_context):
    client = new_context.client
    new_context.host
    register_simulated_host(new_context)
    env = _create_stack(client)

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, "ports": ['6666:7775']}
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc = client.wait_success(svc.activate())
    assert svc.state == "active"
    c1 = _wait_for_compose_instance_start(client, svc, env, "1")

    port11 = c1.ports_link()[0]
    assert port11.publicPort == 6666
    assert port11.privatePort == 7775


def test_project_random_port_update_create(new_context):
    client = new_context.client
    user_client = new_context.user_client
    new_context.host
    register_simulated_host(new_context)
    env = _create_stack(client)
    image_uuid = new_context.image_uuid

    ports = ['6666', '7775', '776']
    launch_config = {"imageUuid": image_uuid, "ports": ports}
    # update the port
    new_range = {"startPort": 65533, "endPort": 65535}
    p = user_client.update(new_context.project,
                           servicesPortRange=new_range)
    p = user_client.wait_success(p)
    assert p.servicesPortRange == new_range

    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    svc = client.wait_success(svc.activate())
    c = _wait_for_compose_instance_start(client, svc, env, "1")
    port = c.ports_link()[0]
    assert port.publicPort is not None
    assert 65533 <= port.publicPort <= 65535

    # try to create service with more ports
    # requested than random range can provide - should be allowed
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.state == 'inactive'

    # create the port
    new_range = {"startPort": 65533, "endPort": 65535}
    project = user_client.create_project(servicesPortRange=new_range)
    project = user_client.wait_success(project)
    assert project.servicesPortRange == new_range


def test_update_port_endpoint(new_context):
    client = new_context.client
    host1 = new_context.host
    env = _create_stack(client)
    hosts = [host1]

    port1 = 5557
    port2 = 5558

    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid, "ports": [str(port1) + ':6666']}
    svc = client.create_service(name=random_str(),
                                environmentId=env.id,
                                launchConfig=launch_config)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"
    svc = client.wait_success(svc.activate(), 120)
    assert svc.state == "active"

    wait_for(lambda: client.reload(svc).publicEndpoints is not None and len(
        client.reload(svc).publicEndpoints) == 1)
    endpoints = client.reload(svc).publicEndpoints
    for host in hosts:
        _validate_endpoint(endpoints, port1, host, svc)

    wait_for(lambda: client.reload(host1).publicEndpoints is not None and len(
        client.reload(host1).publicEndpoints) == 1)
    endpoints = client.reload(host1).publicEndpoints
    _validate_endpoint(endpoints, port1, hosts[0], svc)

    # update port
    c = _wait_for_compose_instance_start(client, svc, env, "1")
    port = c.ports_link()[0]
    assert port.publicPort == port1

    port = client.update(port, publicPort=port2)
    assert port.state == 'updating-active'
    assert port.publicPort == port2
    port = client.wait_success(port)
    assert port.state == 'active'

    # validate endpoints
    wait_for(lambda: client.reload(svc).publicEndpoints is not None and len(
        client.reload(svc).publicEndpoints) == 1)
    endpoints = client.reload(svc).publicEndpoints
    for host in hosts:
        _validate_endpoint(endpoints, port2, host, svc)

    wait_for(lambda: client.reload(host1).publicEndpoints is not None and len(
        client.reload(host1).publicEndpoints) == 1)
    endpoints = client.reload(host1).publicEndpoints
    _validate_endpoint(endpoints, port2, hosts[0], svc)


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


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def _wait_for_compose_instance_start(client, service, env,
                                     number, launch_config_name=None):
    cn = launch_config_name + "_" if \
        launch_config_name is not None else ""
    name = env.name + "_" + service.name + "_" + cn + number

    wait_for(
        lambda: len(client.list_container(name=name, state='running')) > 0
    )
    return client.list_container(name=name, state='running')[0]
