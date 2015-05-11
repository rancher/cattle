from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return nsp


def random_str():
    return 'random{0}'.format(random_num())


def create_env_and_svc(super_client, admin_client, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"
    return service, env


def test_activate_single_service(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    host = sim_context['host']
    container1 = admin_client.create_container(imageUuid=image_uuid,
                                               startOnCreate=False)
    container1 = admin_client.wait_success(container1)

    container2 = admin_client.create_container(imageUuid=image_uuid,
                                               startOnCreate=False)
    container2 = admin_client.wait_success(container2)

    caps = ["SYS_MODULE"]

    restart_policy = {"maximumRetryCount": 2, "name": "on-failure"}

    dns = ['8.8.8.8', '1.2.3.4']

    launch_config = {"imageUuid": image_uuid}

    consumed_service = super_client.create_service(name=random_str(),
                                                   environmentId=env.id,
                                                   networkId=nsp.networkId,
                                                   launchConfig=launch_config)
    consumed_service = super_client.wait_success(consumed_service)

    reg_cred = _create_registry_credential(admin_client)

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
                     "registryCredentialId": reg_cred.id,
                     "requestedHostId": host.id}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)

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

    # activate the service and validate that parameters were set for instance
    service = wait_success(super_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    instances = super_client. \
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
    assert container.registryCredentialId == reg_cred.id
    assert container.cpuSet == "2"
    assert container.requestedHostId == host.id


def test_activate_services(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)
    assert service1.state == "inactive"

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)
    assert service2.state == "inactive"

    env = env.activateservices()
    service1 = super_client.wait_success(service1, 120)
    service2 = super_client.wait_success(service2, 120)
    assert service1.state == "active"
    assert service2.state == "active"


def _validate_instance_stopped(service, super_client, env):
    instances = super_client. \
        list_container(name=env.name + "_" + service.name + "_" + "1")
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        super_client, instance, _resource_is_stopped,
        lambda x: 'State is: ' + x.state)


def _validate_compose_instance_removed(super_client, service, env, number="1"):
    instances = super_client. \
        list_container(name=env.name + "_" + service.name + "_" + number)
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        super_client, instance, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _validate_instance_removed(super_client, service, name):
    instances = super_client. \
        list_container(name=name)
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        super_client, instance, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_deactivate_remove_service(super_client, admin_client,
                                   sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"
    service = wait_success(super_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(super_client, service, env, "1")

    # deactivate service
    service = wait_success(super_client, service.deactivate())
    assert service.state == "inactive"
    _validate_instance_stopped(service, super_client, env)

    # remove service
    service = wait_success(super_client, service.remove())
    _validate_compose_instance_removed(super_client, service, env)


def test_env_deactivate_services(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)
    assert service1.state == "inactive"

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)
    assert service2.state == "inactive"

    # activate services
    env = env.activateservices()
    service1 = super_client.wait_success(service1, 120)
    service2 = super_client.wait_success(service2, 120)
    assert service1.state == "active"
    assert service2.state == "active"

    # deactivate services
    env.deactivateservices()
    service1 = super_client.wait_success(service1)
    service2 = super_client.wait_success(service2)
    assert service1.state == "inactive"
    assert service2.state == "inactive"
    _validate_instance_stopped(service1, super_client, env)
    _validate_instance_stopped(service2, super_client, env)


def test_remove_inactive_service(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service = wait_success(super_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(super_client, service, env, "1")

    # deactivate service
    service = wait_success(super_client, service.deactivate())
    assert service.state == "inactive"

    # remove service
    service = wait_success(super_client, service.remove())
    assert service.state == "removed"
    _validate_compose_instance_removed(super_client, service, env)


def test_remove_environment(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env = env.activateservices()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(super_client, service, env, "1")

    # deactivate services
    env = env.deactivateservices()
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # remove environment
    env = wait_success(admin_client, env.remove())
    assert env.state == "removed"
    wait_for_condition(
        super_client, service, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_create_duplicated_services(super_client, admin_client,
                                    sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}
    service_name = random_str()
    service1 = super_client.create_service(name=service_name,
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    super_client.wait_success(service1)

    with pytest.raises(ApiError) as e:
        super_client.create_service(name=service_name,
                                    environmentId=env.id,
                                    networkId=nsp.networkId,
                                    launchConfig=launch_config)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'name'


def test_service_add_remove_service_link(super_client, admin_client,
                                         sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    # link service2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # remove service link
    service1 = service1.removeservicelink(serviceId=service2.id)
    _validate_remove_service_link(service1, service2, super_client)


def test_link_service_twice(super_client, admin_client,
                            sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    # link servic2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'serviceId'


def test_links_after_service_remove(super_client, admin_client,
                                    sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}
    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    # link servic2 to service1
    service1 = service1.addservicelink(serviceId=service2.id)
    _validate_add_service_link(service1, service2, super_client)

    # link service1 to service2
    service2 = service2.addservicelink(serviceId=service1.id)
    _validate_add_service_link(service2, service1, super_client)

    # remove service1
    service1 = wait_success(super_client, service1.remove())

    _validate_remove_service_link(service1, service2, super_client)

    _validate_remove_service_link(service2, service1, super_client)


def test_link_volumes(super_client, admin_client,
                      sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)
    service1 = wait_success(super_client, service1.activate(), 120)
    container1 = _validate_compose_instance_start(super_client,
                                                  service1, env, "1")

    external_container = super_client.create_container(
        imageUuid=image_uuid,
        requestedHostId=container1.hosts()[0].id)
    external_container = super_client.wait_success(external_container)

    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFrom": [external_container.id],
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service2 = super_client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id])

    service2 = super_client.wait_success(service2)
    service2 = wait_success(super_client, service2.activate(), 120)
    container2 = _validate_compose_instance_start(super_client,
                                                  service2, env, "1")

    # verify that the instance started in service2,
    # got volume of instance of service1
    assert len(container2.dataVolumesFrom) == 2
    assert set(container2.dataVolumesFrom) == set([external_container.id,
                                                   container1.id])


def test_volumes_service_links_scale_one(super_client, admin_client,
                                         sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service2 = super_client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id])
    service2 = super_client.wait_success(service2)

    service3 = super_client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id, service2.id])
    service3 = super_client.wait_success(service3)

    service1 = wait_success(super_client, service1.activate(), 120)
    service2 = super_client.wait_success(service2, 120)
    service3 = super_client.wait_success(service3, 120)

    assert service1.state == "active"
    assert service3.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    s1_container = _validate_compose_instance_start(super_client,
                                                    service1, env, "1")
    s2_container = _validate_compose_instance_start(super_client,
                                                    service2, env, "1")
    s3_container = _validate_compose_instance_start(super_client,
                                                    service3, env, "1")

    assert len(s2_container.dataVolumesFrom) == 1
    assert set(s2_container.dataVolumesFrom) == set([s1_container.id])

    assert len(s3_container.dataVolumesFrom) == 2
    assert set(s3_container.dataVolumesFrom) == set([s1_container.id,
                                                     s2_container.id])


def test_volumes_service_links_scale_two(super_client, admin_client,
                                         sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config,
                                           scale=2)
    service1 = super_client.wait_success(service1)

    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}
    service2 = super_client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id],
                       scale=2)
    service2 = super_client.wait_success(service2)

    service1 = wait_success(super_client, service1.activate(), 120)
    service2 = super_client.wait_success(service2, 120)

    assert service1.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    _validate_compose_instance_start(super_client,
                                     service1, env, "1")
    _validate_compose_instance_start(super_client,
                                     service1, env, "2")
    s21_container = _validate_compose_instance_start(super_client,
                                                     service2, env, "1")
    s22_container = _validate_compose_instance_start(super_client,
                                                     service2, env, "2")

    assert len(s22_container.dataVolumesFrom) == 1
    assert len(s21_container.dataVolumesFrom) == 1


def test_remove_active_service(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service = wait_success(super_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(super_client, service, env, "1")

    # remove service
    service = wait_success(super_client, service.remove(), 120)
    assert service.state == "removed"
    _validate_compose_instance_removed(super_client, service, env)


def _wait_until_active_map_count(service, count, super_client, timeout=30):
    # need this function because agent state changes
    # active->deactivating->removed
    start = time.time()
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    while len(instance_service_map) != count:
        time.sleep(.5)
        instance_service_map = super_client. \
            list_serviceExposeMap(serviceId=service.id, state="active")
        if time.time() - start > timeout:
            assert 'Timeout waiting for map to be removed.'

    return


def test_remove_environment_w_active_svcs(super_client,
                                          admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env = env.activateservices()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    _validate_compose_instance_start(super_client, service, env, "1")

    # remove environment
    env = wait_success(admin_client, env.remove())
    assert env.state == "removed"
    service = super_client.wait_success(service)
    _validate_compose_instance_removed(super_client, service, env)


def _validate_compose_instance_start(super_client, service, env, number):
    instances = super_client. \
        list_container(name=env.name + "_" + service.name + "_" + number,
                       state="running")
    assert len(instances) == 1
    return instances[0]


def _validate_instance_start(service, super_client, name):
    instances = super_client. \
        list_container(name=name)
    assert len(instances) == 1
    return instances[0]


def test_validate_service_scaleup_scaledown(super_client,
                                            admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config,
                                          scale=2)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # scale up the inactive service
    service = super_client.update(service, scale=3, name=service.name)
    service = super_client.wait_success(service, 120)
    assert service.state == "inactive"
    assert service.scale == 3

    # activate services
    env.activateservices()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"

    _validate_compose_instance_start(super_client, service, env, "1")
    instance2 = _validate_compose_instance_start(super_client, service,
                                                 env, "2")
    instance3 = _validate_compose_instance_start(super_client, service,
                                                 env, "3")

    # stop the instance2
    instance2 = wait_success(super_client, instance2)
    instance2 = wait_success(super_client, instance2.stop())
    assert instance2.state == 'stopped'

    # rename the instance 3
    instance3 = super_client.update(instance3, name='newName')

    # scale up the service
    # instance 2 should get started; env_service_3 name should be utilized
    service = super_client.update(service, scale=4, name=service.name)
    service = super_client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 4

    _validate_compose_instance_start(super_client, service, env, "1")
    _validate_compose_instance_start(super_client, service, env, "2")
    _validate_compose_instance_start(super_client, service, env, "3")
    _validate_instance_start(service, super_client, instance3.name)

    # scale down the service
    service = super_client.update(service, scale=2, name=service.name)
    service = super_client.wait_success(service, 120)
    assert service.state == "active"
    # validate that only 2 service instance mappings exist
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 2


def test_link_services_from_diff_env(super_client, admin_client,
                                     sim_context, nsp):
    env1 = admin_client.create_environment(name=random_str())
    env1 = admin_client.wait_success(env1)

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env1.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    env2 = admin_client.create_environment(name=random_str())
    env2 = admin_client.wait_success(env2)
    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env2.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    # try to link
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'serviceId'


def test_set_service_links(super_client, admin_client,
                           sim_context, nsp):
    env1 = admin_client.create_environment(name=random_str())
    env1 = admin_client.wait_success(env1)

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env1.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env1.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    service3 = super_client.create_service(name=random_str(),
                                           environmentId=env1.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service3 = super_client.wait_success(service3)

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
    env2 = admin_client.create_environment(name=random_str())
    env2 = admin_client.wait_success(env2)

    service4 = super_client.create_service(name=random_str(),
                                           environmentId=env2.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service4 = super_client.wait_success(service4)

    with pytest.raises(ApiError) as e:
        service1.setservicelinks(serviceIds=[service4.id])

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'serviceId'


def _instance_remove(instance, super_client):
    instance = wait_success(super_client, instance)
    instance = wait_success(super_client, instance.stop())
    assert instance.state == 'stopped'
    instance = wait_success(super_client, instance.remove())
    assert instance.state == 'removed'
    return instance


def test_destroy_service_instance(super_client,
                                  admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config,
                                          scale=3)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate service
    service.activate()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"

    instance1 = _validate_compose_instance_start(super_client, service,
                                                 env, "1")
    instance2 = _validate_compose_instance_start(super_client, service,
                                                 env, "2")
    instance3 = _validate_compose_instance_start(super_client, service,
                                                 env, "3")

    return

    # 1. stop and remove the instance2. Validate the mapping still exist
    instance2 = _instance_remove(instance2, super_client)

    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance2.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_active,
        lambda x: 'State is: ' + x.state)

    # 2. deactivate the service
    service.deactivate()
    service = super_client.wait_success(service, 120)
    assert service.state == "inactive"

    # 3. activate the service. The map should be gone
    service.activate()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"

    # 4. destroy instance3 and update the service's scale.
    # Validate that instance3 map is gone
    instance3 = _instance_remove(instance3, super_client)
    service = super_client.update(service, scale=4, name=service.name)
    service = super_client.wait_success(service, 120)

    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance3.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    # purge the instance1 w/o changing the service
    # and validate instance1-service map is gone
    instance1 = _instance_remove(instance1, super_client)
    instance1 = wait_success(super_client, instance1.purge())
    assert instance1.state == 'purged'
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, instanceId=instance1.id)
    assert len(instance_service_map) == 1
    wait_for_condition(
        super_client, instance_service_map[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_service_rename(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config,
                                          scale=2)
    service = super_client.wait_success(service)

    # activate service
    service.activate()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"

    _validate_compose_instance_start(super_client, service, env, "1")
    _validate_compose_instance_start(super_client, service, env, "2")

    # update name and validate that the service name got
    # updated as well as its instances
    new_name = "newname"
    service = super_client.update(service, scale=3, name=new_name)
    service = super_client.wait_success(service)
    assert service.name == new_name
    _validate_compose_instance_start(super_client, service, env, "1")
    _validate_compose_instance_start(super_client, service, env, "2")


def test_env_rename(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service_1 = super_client.create_service(name=random_str(),
                                            environmentId=env.id,
                                            networkId=nsp.networkId,
                                            launchConfig=launch_config,
                                            scale=2)
    service_1 = super_client.wait_success(service_1)

    service_2 = super_client.create_service(name=random_str(),
                                            environmentId=env.id,
                                            networkId=nsp.networkId,
                                            launchConfig=launch_config,
                                            scale=1)
    service_2 = super_client.wait_success(service_2)

    # activate services
    env = env.activateservices()
    service_1 = super_client.wait_success(service_1, 120)
    service_2 = super_client.wait_success(service_2, 120)
    assert service_1.state == "active"
    assert service_2.state == "active"

    _validate_compose_instance_start(super_client, service_1, env, "1")
    _validate_compose_instance_start(super_client, service_1, env, "2")
    _validate_compose_instance_start(super_client, service_2, env, "1")

    # update env name and validate that the
    # env name got updated as well as all instances
    new_name = "newname"
    env = admin_client.update(env, name=new_name)
    env = admin_client.wait_success(env)
    assert env.name == new_name
    _validate_compose_instance_start(super_client, service_1, env, "1")
    _validate_compose_instance_start(super_client, service_1, env, "2")
    _validate_compose_instance_start(super_client, service_2, env, "1")


def test_validate_scale_down_restore_state(super_client,
                                           admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config,
                                          scale=3)
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # activate services
    env.activateservices()
    service = super_client.wait_success(service, 120)
    assert service.state == "active"

    instance1 = _validate_compose_instance_start(super_client, service,
                                                 env, "1")
    instance2 = _validate_compose_instance_start(super_client, service,
                                                 env, "2")
    instance3 = _validate_compose_instance_start(super_client, service,
                                                 env, "3")
    # stop the instances 1, 2 and destroy instance 3
    instance1 = wait_success(super_client, instance1.stop())
    assert instance1.state == 'stopped'
    instance2 = wait_success(super_client, instance2.stop())
    assert instance2.state == 'stopped'
    instance3 = _instance_remove(instance3, super_client)
    assert instance3.state == 'removed'

    # scale down the service and validate that:
    # first instance is running
    # second instance is removed
    # third instance is removed
    service = super_client.update(service, scale=1, name=service.name)
    super_client.wait_success(service)

    # validate that only one service instance mapping exists
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 1


def test_validate_labels(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create service1 with labels defined
    service_name1 = random_str()
    initial_labels1 = {'affinity': "container==B", '!affinity': "container==C"}
    image_uuid = sim_context['imageUuid']
    launch_config1 = {"imageUuid": image_uuid, "labels": initial_labels1}

    service1 = super_client.create_service(name=service_name1,
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config1)
    service1 = super_client.wait_success(service1)
    assert service1.state == "inactive"
    assert service1.launchConfig.labels == initial_labels1

    # create service2 w/o labels defined
    service_name2 = random_str()
    image_uuid = sim_context['imageUuid']
    launch_config2 = {"imageUuid": image_uuid}

    service2 = super_client.create_service(name=service_name2,
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config2)
    service2 = super_client.wait_success(service2)
    assert service2.state == "inactive"
    assert "labels" not in service2.launchConfig

    # activate services
    env.activateservices()
    service1 = super_client.wait_success(service1, 120)
    assert service1.state == "active"
    service2 = super_client.wait_success(service2, 120)
    assert service2.state == "active"

    # check that labels defined in launch config + the internal label, are set
    result_labels_1 = {'affinity': "container==B", '!affinity': "container==C",
                       'io.rancher.service.name': service_name1,
                       'io.rancher.environment.name': env.name}
    instance1 = _validate_compose_instance_start(super_client, service1,
                                                 env, "1")
    all(item in instance1.labels for item in result_labels_1)

    # check that only one internal label is set
    result_labels_2 = {'io.rancher.service.name': service_name2,
                       'io.rancher.environment.name': env.name}
    instance2 = _validate_compose_instance_start(super_client, service2,
                                                 env, "1")
    all(item in instance2.labels for item in result_labels_2)


def test_sidekick_services_activate(super_client,
                                    admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    # service3 with a diff sidekick label, and service4 with no label
    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service2 = super_client.wait_success(service2)

    launch_config1 = {"imageUuid": image_uuid}
    service3 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config1)
    service3 = super_client.wait_success(service3)

    launch_config2 = {"imageUuid": image_uuid,
                      "labels": {'io.rancher.service.sidekick': "random123"}}
    service4 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config2)
    service4 = super_client.wait_success(service4)

    # activate service1, service 2 should be activated too
    service1 = wait_success(super_client, service1.activate(), 120)
    assert service1.state == "active"
    service2 = super_client.wait_success(service2, 120)
    assert service2.state == "active"

    # service 3 and 4 should be inactive
    service3 = super_client.wait_success(service3)
    assert service3.state == "inactive"
    service4 = super_client.wait_success(service4)
    assert service4.state == "inactive"


def test_sidekick_restart_instances(super_client,
                                    admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config,
                                           scale=2)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config, scale=2)
    service2 = super_client.wait_success(service2)

    # activate service1, service 2 should be activated too
    service1 = wait_success(super_client, service1.activate(), 120)
    assert service1.state == "active"
    service2 = super_client.wait_success(service2, 120)
    assert service2.state == "active"

    instance11 = _validate_compose_instance_start(super_client,
                                                  service1, env, "1")
    _validate_compose_instance_start(super_client, service1, env, "2")
    _validate_compose_instance_start(super_client, service2, env, "1")
    instance22 = _validate_compose_instance_start(super_client,
                                                  service2, env, "2")

    instance_service_map1 = super_client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = super_client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2

    # stop instance11, destroy instance12 and call update on a service1
    # scale should be restored
    wait_success(super_client, instance11.stop())
    _instance_remove(instance22, super_client)
    service1 = super_client.update(service1, scale=2, name=service1.name)
    service1 = super_client.wait_success(service1, 120)

    _validate_compose_instance_start(super_client, service1, env, "1")
    _validate_compose_instance_start(super_client, service1, env, "2")
    _validate_compose_instance_start(super_client, service2, env, "1")
    _validate_compose_instance_start(super_client, service2, env, "2")

    instance_service_map1 = super_client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = super_client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2


def test_sidekick_scaleup(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined
    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config,
                                           scale=1)
    service1 = super_client.wait_success(service1)

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config, scale=1)
    service2 = super_client.wait_success(service2)

    # activate service1, service 2 should be activated too
    service1 = wait_success(super_client, service1.activate(), 120)
    assert service1.state == "active"
    service2 = super_client.wait_success(service2, 120)
    assert service2.state == "active"

    _validate_compose_instance_start(super_client, service1, env, "1")
    _validate_compose_instance_start(super_client, service2, env, "1")

    # scale up service1, verify that the service 2 was scaled up and updated
    service1 = super_client.update(service1, scale=2, name=service1.name)
    _wait_compose_instance_start(super_client, service1, env, "1")
    _wait_compose_instance_start(super_client, service1, env, "2")
    _wait_compose_instance_start(super_client, service2, env, "1")
    _wait_compose_instance_start(super_client, service2, env, "2")

    service1 = super_client.wait_success(service1, 120)
    assert service1.state == "active"
    assert service1.scale == 2
    service2 = super_client.wait_success(service2, 120)
    assert service2.state == "active"
    assert service2.scale == 2

    instance_service_map1 = super_client. \
        list_serviceExposeMap(serviceId=service1.id, state="active")
    assert len(instance_service_map1) == 2

    instance_service_map2 = super_client. \
        list_serviceExposeMap(serviceId=service2.id, state="active")
    assert len(instance_service_map2) == 2


def test_sidekick_diff_scale(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name=random_str())
    env = admin_client.wait_success(env)
    assert env.state == "active"

    # create service1/service2 with the same sidekick label defined,
    # but diff scale - should fail
    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid,
                     "labels": {'io.rancher.service.sidekick': "random"}}

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config,
                                           scale=2)
    service1 = super_client.wait_success(service1)
    assert service1.scale == 2

    service2 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config,
                                           scale=3)
    service2 = super_client.wait_success(service2)
    assert service2.scale == 2


def _wait_compose_instance_start(super_client, service,
                                 env, number, timeout=30):
    start = time.time()
    instances = super_client. \
        list_container(name=env.name + "_" + service.name + "_" + number,
                       state="running")
    while len(instances) != 1:
        time.sleep(.5)
        instances = super_client. \
            list_container(name=env.name + "_" + service.name + "_" + number,
                           state="running")
        if time.time() - start > timeout:
            assert 'Timeout waiting for instance to become running.'


def _create_registry_credential(admin_client):
    registry = _create_registry(admin_client)
    reg_cred = admin_client.create_registry_credential(
        registryId=registry.id,
        email='test@rancher.com',
        publicValue='wizardofmath+whisper',
        secretValue='W0IUYDBM2VORHM4DTTEHSMKLXGCG3KD3IT081QWWTZA11R9DZS2DDPP72'
                    '48NUTT6')
    assert reg_cred is not None
    assert reg_cred.email == 'test@rancher.com'
    assert reg_cred.kind == 'registryCredential'
    assert reg_cred.registryId == registry.id
    assert reg_cred.publicValue == 'wizardofmath+whisper'
    assert 'secretValue' not in reg_cred

    return reg_cred


def _create_registry(admin_client):
    registry = admin_client.create_registry(serverAddress='quay.io',
                                            name='Quay')
    assert registry.serverAddress == 'quay.io'
    assert registry.name == 'Quay'

    return registry


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
