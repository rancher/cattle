from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def config_id(admin_client):
    default_lb_config = admin_client. \
        create_loadBalancerConfig(name=random_str())
    default_lb_config = admin_client.wait_success(default_lb_config)
    return default_lb_config.id


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)
    return nsp


# test (C)
def test_lb_create_wo_config(admin_client):
    with pytest.raises(ApiError) as e:
        admin_client.create_loadBalancer(name=random_str())

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerConfigId'


# test (C)
def create_valid_lb(admin_client, config_id, sim_context, super_client, nsp):
    im_id = sim_context['imageUuid']
    test_lb = super_client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config_id,
                            loadBalancerInstanceImageUuid=im_id,
                            loadBalancerInstanceUriPredicate='sim://',
                            networkId=nsp.networkId)

    test_lb = super_client.wait_success(test_lb)
    return test_lb


def test_lb_create_w_config(admin_client, config_id, sim_context,
                            super_client, nsp):
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    assert lb.state == 'active'
    assert lb.loadBalancerConfigId == config_id


# test (D)
def test_lb_remove(admin_client, config_id, sim_context, super_client, nsp):
    # create lb
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    # remove newly created lb
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'


# test (U)
def test_lb_update(admin_client, config_id, sim_context, super_client, nsp):
    # create lb
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    # update the lb
    lb = admin_client.update(lb, name='newName')
    assert lb.name == 'newName'


def test_lb_add_target_instance(admin_client, sim_context, config_id,
                                super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)

    validate_add_target(admin_client, container, lb, super_client)


def test_lb_remove_target_instance(admin_client, sim_context, config_id,
                                   super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(instanceId=container.id)
    validate_remove_target(admin_client, container, lb, super_client)


def validate_add_target_ip(admin_client, ip_address, lb, super_client):
    target_maps = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert target_map.ipAddress == ip_address


def test_lb_add_target_ip_address(admin_client, sim_context, config_id,
                                  super_client, nsp):
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)
    ip_address = "10.1.1.1"
    lb = lb.addtarget(ipAddress=ip_address)
    lb = admin_client.wait_success(lb)

    validate_add_target_ip(admin_client, ip_address, lb, super_client)


def validate_remove_target_ip(admin_client, ip_address, lb, super_client):
    target_maps = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_lb_remove_target_ip_address(admin_client, sim_context, config_id,
                                     super_client, nsp):
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    # add target to a load balancer and verify that it got created
    ip_address = "10.1.1.1"
    lb = lb.addtarget(ipAddress=ip_address)

    validate_add_target_ip(admin_client, ip_address, lb, super_client)

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(ipAddress="10.1.1.1")

    validate_remove_target_ip(admin_client, ip_address, lb, super_client)


def create_lb_and_container(admin_client, sim_context, config_id,
                            super_client, nsp):
    image_uuid = sim_context['imageUuid']
    # create load balancer
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)
    # create a container, no need to start it
    container = admin_client.create_container(imageUuid=image_uuid,
                                              startOnCreate=False)
    container = admin_client.wait_success(container)
    return container, lb


def test_lb_remove_w_target(admin_client, sim_context, config_id,
                            super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    lb = admin_client.wait_success(lb)

    # remove the load balancer
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'

    validate_remove_target(admin_client, container, lb, super_client)


def test_lb_remove_w_host(admin_client, super_client, sim_context,
                          config_id, nsp):
    host = sim_context['host']
    # create lb, assign the hosts to it
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    lb = lb.addhost(hostId=host.id)
    validate_add_host(host, lb, super_client)

    # remove the load balancer
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'

    validate_remove_host(host, lb, super_client)


def validate_add_target(admin_client, container1, lb, super_client):
    target_maps = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def validate_remove_target(admin_client, container2, lb, super_client):
    target_maps = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container2.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_set_target_instance(admin_client, sim_context, config_id,
                             super_client, nsp):
    container1, lb = create_lb_and_container(admin_client,
                                             sim_context, config_id,
                                             super_client, nsp)

    container2 = admin_client. \
        create_container(imageUuid=sim_context['imageUuid'],
                         startOnCreate=False)
    container2 = admin_client.wait_success(container2)

    # set 2 targets
    lb = lb.settargets(instanceIds=[container1.id, container2.id])
    lb = admin_client.wait_success(lb)

    validate_add_target(admin_client, container1, lb, super_client)

    validate_add_target(admin_client, container2, lb, super_client)

    # set 1 target
    lb = lb.settargets(instanceIds=[container1.id])

    validate_add_target(admin_client, container1, lb, super_client)
    validate_remove_target(admin_client, container2, lb, super_client)

    # set 0 targets
    lb = lb.settargets(instanceIds=[])

    validate_remove_target(admin_client, container1, lb, super_client)


def test_lb_set_target_ip_address(admin_client, sim_context, config_id,
                                  super_client, nsp):
    lb = create_valid_lb(admin_client, config_id, sim_context,
                         super_client, nsp)

    # set 2 targets
    lb = lb.settargets(ipAddresses=["10.1.1.1", "10.1.1.2"])

    validate_add_target_ip(admin_client, "10.1.1.1", lb, super_client)

    validate_add_target_ip(admin_client, "10.1.1.2", lb, super_client)

    # set 1 target
    lb = lb.settargets(ipAddresses=["10.1.1.1"])

    validate_add_target_ip(admin_client, "10.1.1.1", lb, super_client)

    validate_remove_target_ip(admin_client, "10.1.1.2", lb, super_client)

    # set 0 targets
    lb = lb.settargets(ipAddresses=[])

    validate_remove_target_ip(admin_client, "10.1.1.1", lb, super_client)


def test_set_target_instance_and_ip(admin_client, sim_context, config_id,
                                    super_client, nsp):
    container1, lb = create_lb_and_container(admin_client, sim_context,
                                             config_id, super_client, nsp)

    # set 2 targets - one ip and one instanceId
    lb = lb.settargets(instanceIds=[container1.id],
                       ipAddresses="10.1.1.1")

    validate_add_target(admin_client, container1, lb, super_client)

    validate_add_target_ip(admin_client, "10.1.1.1", lb, super_client)


def test_lb_add_target_instance_twice(admin_client, sim_context, config_id,
                                      super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    with pytest.raises(ApiError) as e:
        lb.addtarget(instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_remove_non_existing_target_instance(admin_client, sim_context,
                                                config_id, super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        lb.removetarget(instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_address_and_instance(admin_client, sim_context,
                                               config_id, super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    with pytest.raises(ApiError) as e:
        lb.addtarget(ipAddress="10.1.1.1",
                     instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_add_target_w_no_option(admin_client, sim_context, config_id,
                                   super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    with pytest.raises(ApiError) as e:
        lb.addtarget()

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_twice(admin_client, sim_context, config_id,
                                super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(ipAddress="10.1.1.1")
    validate_add_target_ip(admin_client, "10.1.1.1", lb, super_client)

    with pytest.raises(ApiError) as e:
        lb.addtarget(ipAddress="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_remove_non_existing_target_ip(admin_client, sim_context,
                                          config_id, super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        lb.removetarget(ipAddress="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_add_removed_target_again(admin_client, sim_context, config_id,
                                  super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    # remove the target
    lb = lb.removetarget(instanceId=container.id)
    validate_remove_target(admin_client, container, lb, super_client)

    # add the target - should be allowed
    lb.addtarget(instanceId=container.id)


def test_destroy_container(admin_client, sim_context, config_id,
                           super_client, nsp):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id, super_client, nsp)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    # destroy the instance
    # stop the lb instance
    container = wait_success(admin_client, container)
    if container.state == 'running':
        container = wait_success(admin_client, container.stop())
        assert container.state == 'stopped'

    # remove the lb instance
    container = wait_success(admin_client, container.remove())
    assert container.state == 'removed'

    validate_remove_target(admin_client, container, lb, super_client)


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

    return host_map
