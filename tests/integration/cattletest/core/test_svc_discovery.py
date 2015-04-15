from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return nsp


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
    env = admin_client.create_environment(name="compose")
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
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
                     "command": 'touch test.txt',
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
                     "restartPolicy": restart_policy,
                     "directory": "/",
                     "hostname": "test",
                     "user": "test",
                     "instanceLinks": {
                         'container2_link':
                             container2.id},
                     "registryCredentialId": reg_cred.id}

    service = super_client.create_service(name=random_str(),
                                          environmentId=env.id,
                                          networkId=nsp.networkId,
                                          launchConfig=launch_config)
    service = super_client.wait_success(service)

    # validate that parameters were set for service
    assert service.state == "inactive"
    assert service.launchConfig.imageUuid == image_uuid
    assert service.launchConfig.command == "touch test.txt"
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
    # assert service.launchConfig.registryCredentialId == reg_cred.id

    # activate the service and validate that parameters were set for instance
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    container = instances[0]
    assert container.imageUuid == image_uuid
    assert container.command == "touch test.txt"
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


def test_activate_services(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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


def _validate_instance_stopped(service, super_client):
    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        super_client, instance, _resource_is_stopped,
        lambda x: 'State is: ' + x.state)


def _validate_instance_removed(super_client, service, number="1"):
    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + number)
    assert len(instances) == 1
    instance = instances[0]
    wait_for_condition(
        super_client, instance, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_deactivate_remove_service(super_client, admin_client,
                                   sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    assert instances[0].state == "running"

    # deactivate service
    service = wait_success(admin_client, service.deactivate())
    assert service.state == "inactive"
    _validate_instance_stopped(service, super_client)

    # remove service
    service = wait_success(admin_client, service.remove())
    _validate_instance_removed(super_client, service)


def test_env_deactivate_services(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    _validate_instance_stopped(service1, super_client)
    _validate_instance_stopped(service2, super_client)


def test_remove_inactive_service(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    assert instances[0].state == "running"

    # deactivate service
    service = wait_success(admin_client, service.deactivate())
    assert service.state == "inactive"

    # remove service
    service = wait_success(admin_client, service.remove())
    assert service.state == "removed"
    _validate_instance_removed(super_client, service)


def test_remove_environment(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    assert instances[0].state == "running"

    # deactivate services
    env = env.deactivateservices()
    service = super_client.wait_success(service)
    assert service.state == "inactive"

    # remove environment
    env = wait_success(admin_client, env.remove())
    assert env.state == "removed"
    wait_for_condition(
        admin_client, service, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_create_duplicated_services(super_client, admin_client,
                                    sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    env = admin_client.create_environment(name="compose")
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
    service1 = admin_client.wait_success(service1)

    service_map = super_client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id)

    assert len(service_map) == 1
    assert service_map[0].state == 'active'

    # remove service link
    service1 = service1.removeservicelink(serviceId=service2.id)
    service1 = admin_client.wait_success(service1)

    service_map = super_client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id)

    assert service_map[0].state == 'removed'


def test_link_service_twice(super_client, admin_client,
                            sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service1 = admin_client.wait_success(service1)

    service_map = super_client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id)

    assert len(service_map) == 1
    assert service_map[0].state == 'active'

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'serviceId'


def test_links_after_service_remove(super_client, admin_client,
                                    sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service1 = admin_client.wait_success(service1)

    service_map1 = super_client. \
        list_serviceConsumeMap(serviceId=service1.id,
                               consumedServiceId=service2.id)

    assert len(service_map1) == 1
    assert service_map1[0].state == 'active'

    # link service1 to service2
    service2 = service1.addservicelink(serviceId=service1.id)
    service2 = admin_client.wait_success(service2)

    service_map2 = super_client. \
        list_serviceConsumeMap(serviceId=service2.id,
                               consumedServiceId=service1.id)

    assert len(service_map2) == 1
    assert service_map2[0].state == 'active'

    # remove service1
    service1 = wait_success(admin_client, service1.remove())

    _wait_until_service_map_removed(service1, service2, super_client)

    _wait_until_service_map_removed(service2, service1, super_client)


def test_link_volumes(super_client, admin_client,
                      sim_context, nsp):
    env = admin_client.create_environment(name="compose")
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}
    external_container = admin_client.create_container(imageUuid=image_uuid,
                                                       startOnCreate=False)
    external_container = admin_client.wait_success(external_container)

    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)
    service1 = wait_success(admin_client, service1.activate(), 120)
    instances = super_client. \
        list_container(name="compose_" + service1.name + "_" + "1")
    assert len(instances) == 1
    container1 = instances[0]

    launch_config = {"imageUuid": image_uuid,
                     "dataVolumesFrom": [external_container.id]}

    service2 = super_client. \
        create_service(name=random_str(),
                       environmentId=env.id,
                       networkId=nsp.networkId,
                       launchConfig=launch_config,
                       dataVolumesFromService=[service1.id])

    service2 = super_client.wait_success(service2)
    service2 = wait_success(admin_client, service2.activate(), 120)
    instances = super_client. \
        list_container(name="compose_" + service2.name + "_" + "1")
    assert len(instances) == 1

    # verify that the instance started in service2,
    # got volume of instance of service1
    container2 = instances[0]
    assert len(container2.dataVolumesFrom) == 2
    assert set(container2.dataVolumesFrom) == set([external_container.id,
                                                   container1.id])


def test_volumes_service_links(super_client, admin_client,
                               sim_context, nsp):
    env = admin_client.create_environment(name="compose")
    env = admin_client.wait_success(env)
    assert env.state == "active"

    image_uuid = sim_context['imageUuid']
    launch_config = {"imageUuid": image_uuid}
    service1 = super_client.create_service(name=random_str(),
                                           environmentId=env.id,
                                           networkId=nsp.networkId,
                                           launchConfig=launch_config)
    service1 = super_client.wait_success(service1)

    launch_config = {"imageUuid": image_uuid}
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

    env = env.activateservices()

    # 1. validate services
    service3 = super_client.wait_success(service3, 120)

    # at this point, service 2 and service1 should be activated
    service1 = super_client.reload(service1)
    service2 = super_client.reload(service2)
    assert service3.state == "active"
    assert service1.state == "active"
    assert service2.state == "active"

    # 2. validate instances
    instances = super_client. \
        list_container(name="compose_" + service1.name + "_" + "1")
    assert len(instances) == 1
    s1_container = instances[0]

    instances = super_client. \
        list_container(name="compose_" + service2.name + "_" + "1")
    assert len(instances) == 1
    s2_container = instances[0]

    instances = super_client. \
        list_container(name="compose_" + service3.name + "_" + "1")
    assert len(instances) == 1
    s3_container = instances[0]

    assert len(s2_container.dataVolumesFrom) == 1
    assert set(s2_container.dataVolumesFrom) == set([s1_container.id])

    assert len(s3_container.dataVolumesFrom) == 2
    assert set(s3_container.dataVolumesFrom) == set([s1_container.id,
                                                     s2_container.id])


def test_remove_active_service(super_client, admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)

    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    assert instances[0].state == "running"

    # remove service
    service = wait_success(admin_client, service.remove(), 120)
    assert service.state == "removed"
    _validate_instance_removed(super_client, service)


def validateServiceAndInstances(admin_client, service, super_client):
    service = wait_success(admin_client, service.activate(), 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id, state="active")
    assert len(instance_service_map) == 2
    instances = super_client. \
        list_container(name="compose_"
                            + service.name + "_" + "1", state="running")
    assert len(instances) == 1
    container1 = instances[0]
    assert container1.state == "running"
    instances = super_client. \
        list_container(name="compose_"
                            + service.name + "_" + "2", state="running")
    assert len(instances) == 1
    container2 = instances[0]
    assert container2.state == "running"
    return container1, service


def test_destroy_service_instance(super_client,
                                  admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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

    # 1. activate services, wait for instances to come up
    container1, service = validateServiceAndInstances(admin_client,
                                                      service, super_client)

    # 2. destroy instance
    container1 = wait_success(super_client, container1.stop())
    assert container1.state == 'stopped'
    container1 = wait_success(super_client, container1.remove())
    assert container1.state == 'removed'

    # 3. deactivate service
    service = wait_success(admin_client, service.deactivate(), 120)
    assert service.state == "inactive"

    # 4. activate service again and verify that both instances are present
    validateServiceAndInstances(admin_client, service, super_client)


def test_remove_environment_w_active_svcs(super_client,
                                          admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    instance_service_map = super_client. \
        list_serviceExposeMap(serviceId=service.id)
    assert len(instance_service_map) == 1
    assert instance_service_map[0].state == 'active'

    instances = super_client. \
        list_container(name="compose_" + service.name + "_" + "1")
    assert len(instances) == 1
    assert instances[0].state == "running"

    # remove environment
    env = wait_success(admin_client, env.remove())
    assert env.state == "removed"
    service = super_client.wait_success(service)
    _validate_instance_removed(super_client, service)


def _validate_compose_instance_start(service, super_client, number):
    instances = super_client. \
        list_container(name="compose_" + service.name
                            + "_" + number, state="running")
    assert len(instances) == 1
    return instances[0]


def test_valiate_service_scaleup_scaledown(super_client,
                                           admin_client, sim_context, nsp):
    env = admin_client.create_environment(name="compose")
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
    service = admin_client.update(service, scale=3)
    service = admin_client.wait_success(service, 120)
    assert service.state == "inactive"
    assert service.scale == 3

    # activate services
    env.activateservices()
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"

    _validate_compose_instance_start(service, super_client, "1")
    instance2 = _validate_compose_instance_start(service, super_client, "2")
    _validate_compose_instance_start(service, super_client, "3")

    # destroy the instance2
    instance2 = wait_success(super_client, instance2)
    instance2 = wait_success(super_client, instance2.stop())
    assert instance2.state == 'stopped'

    instance2 = wait_success(super_client, instance2.remove())
    assert instance2.state == 'removed'

    # scale up the service
    service = admin_client.update(service, scale=4)
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    assert service.scale == 4

    _validate_compose_instance_start(service, super_client, "1")
    _validate_compose_instance_start(service, super_client, "2")
    _validate_compose_instance_start(service, super_client, "3")
    _validate_compose_instance_start(service, super_client, "4")

    # scale down the service
    service = admin_client.update(service, scale=2)
    service = admin_client.wait_success(service, 120)
    assert service.state == "active"
    _validate_compose_instance_start(service, super_client, "1")
    _validate_compose_instance_start(service, super_client, "2")
    _validate_instance_removed(super_client, service, "3")
    _validate_instance_removed(super_client, service, "4")


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

    # try to link again
    with pytest.raises(ApiError) as e:
        service1.addservicelink(serviceId=service2.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidReference'
    assert e.value.error.fieldName == 'serviceId'


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


def _resource_is_removed(resource):
    return resource.state == 'removed'


def _wait_until_service_map_removed(service, consumed_service,
                                    super_client, timeout=30):
    # need this function because we can't
    # use wait_for_condition for resource of type map

    start = time.time()
    service_maps = super_client. \
        list_serviceConsumeMap(serviceId=service.id,
                               consumedServiceId=consumed_service.id)
    service_map = service_maps[0]
    while service_map.state != 'removed':
        time.sleep(.5)
        service_maps = super_client. \
            list_serviceConsumeMap(serviceId=service.id,
                                   consumedServiceId=consumed_service.id)
        service_map = service_maps[0]
        if time.time() - start > timeout:
            assert 'Timeout waiting for service map to be removed.'

    return
