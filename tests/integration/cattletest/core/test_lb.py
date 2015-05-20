from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def config_id(client):
    default_lb_config = client. \
        create_loadBalancerConfig(name=random_str())
    default_lb_config = client.wait_success(default_lb_config)
    return default_lb_config.id


# test (C)
def test_lb_create_wo_config(client):
    with pytest.raises(ApiError) as e:
        client.create_loadBalancer(name=random_str())

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerConfigId'


# test (C)
def create_valid_lb(client, config_id):
    test_lb = client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config_id)

    test_lb = client.wait_success(test_lb)
    return test_lb


def test_lb_create_w_config(client, config_id):
    lb = create_valid_lb(client, config_id)

    assert lb.state == 'active'
    assert lb.loadBalancerConfigId == config_id


# test (D)
def test_lb_remove(client, config_id):
    # create lb
    lb = create_valid_lb(client, config_id)

    # remove newly created lb
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'


# test (U)
def test_lb_update(client, config_id):
    # create lb
    lb = create_valid_lb(client, config_id)

    # update the lb
    lb = client.update(lb, name='newName')
    assert lb.name == 'newName'


def test_lb_add_target_instance(super_client, client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)

    validate_add_target(container, lb, super_client)


def test_lb_remove_target_instance(super_client, client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(container, lb, super_client)

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(instanceId=container.id)
    validate_remove_target(container, lb, super_client)


def validate_add_target_ip(ip_address, lb, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert target_map.ipAddress == ip_address


def test_lb_add_target_ip_address(client, context, config_id, super_client):
    lb = create_valid_lb(client, config_id)
    ip_address = "10.1.1.1"
    lb = lb.addtarget(ipAddress=ip_address)
    lb = super_client.wait_success(lb)

    validate_add_target_ip(ip_address, lb, super_client)


def validate_remove_target_ip(ip_address, lb, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_lb_remove_target_ip_address(client, context, config_id):
    lb = create_valid_lb(client, config_id)

    # add target to a load balancer and verify that it got created
    ip_address = "10.1.1.1"
    lb = lb.addtarget(ipAddress=ip_address)

    validate_add_target_ip(ip_address, lb, client)

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(ipAddress="10.1.1.1")

    validate_remove_target_ip(ip_address, lb, client)


def create_lb_and_container(client, context, config_id):
    # create load balancer
    lb = create_valid_lb(client, config_id)

    # create a container, no need to start it
    container = client.create_container(imageUuid=context.image_uuid,
                                        startOnCreate=False)
    container = client.wait_success(container)
    return container, lb


def test_lb_remove_w_target(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    lb = client.wait_success(lb)

    # remove the load balancer
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'

    validate_remove_target(container, lb, client)


def test_lb_remove_w_host(client, context, config_id):
    host = context.host
    # create lb, assign the hosts to it
    lb = create_valid_lb(client, config_id)

    lb = lb.addhost(hostId=host.id)
    validate_add_host(host, lb, client)

    # remove the load balancer
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'

    validate_remove_host(host, lb, client)


def validate_add_target(container1, lb, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def validate_remove_target(container2, lb, super_client):
    target_maps = super_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container2.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def test_set_target_instance(client, context, config_id):
    container1, lb = create_lb_and_container(client, context, config_id)

    container2 = client. \
        create_container(imageUuid=context.image_uuid,
                         startOnCreate=False)
    container2 = client.wait_success(container2)

    # set 2 targets
    lb = lb.settargets(instanceIds=[container1.id, container2.id])
    lb = client.wait_success(lb)

    validate_add_target(container1, lb, client)

    validate_add_target(container2, lb, client)

    # set 1 target
    lb = lb.settargets(instanceIds=[container1.id])

    validate_add_target(container1, lb, client)
    validate_remove_target(container2, lb, client)

    # set 0 targets
    lb = lb.settargets(instanceIds=[])

    validate_remove_target(container1, lb, client)


def test_lb_set_target_ip_address(client, context, config_id):
    lb = create_valid_lb(client, config_id)

    # set 2 targets
    lb = lb.settargets(ipAddresses=["10.1.1.1", "10.1.1.2"])

    validate_add_target_ip("10.1.1.1", lb, client)

    validate_add_target_ip("10.1.1.2", lb, client)

    # set 1 target
    lb = lb.settargets(ipAddresses=["10.1.1.1"])

    validate_add_target_ip("10.1.1.1", lb, client)

    validate_remove_target_ip("10.1.1.2", lb, client)

    # set 0 targets
    lb = lb.settargets(ipAddresses=[])

    validate_remove_target_ip("10.1.1.1", lb, client)


def test_set_target_instance_and_ip(client, context, config_id):
    container1, lb = create_lb_and_container(client, context, config_id)

    # set 2 targets - one ip and one instanceId
    lb = lb.settargets(instanceIds=[container1.id],
                       ipAddresses="10.1.1.1")

    validate_add_target(container1, lb, client)

    validate_add_target_ip("10.1.1.1", lb, client)


def test_lb_add_target_instance_twice(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(container, lb, client)

    with pytest.raises(ApiError) as e:
        lb.addtarget(instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_remove_non_existing_target_instance(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        lb.removetarget(instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_address_and_instance(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    with pytest.raises(ApiError) as e:
        lb.addtarget(ipAddress="10.1.1.1",
                     instanceId=container.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_add_target_w_no_option(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    with pytest.raises(ApiError) as e:
        lb.addtarget()

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_twice(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(ipAddress="10.1.1.1")
    validate_add_target_ip("10.1.1.1", lb, client)

    with pytest.raises(ApiError) as e:
        lb.addtarget(ipAddress="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_remove_non_existing_target_ip(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        lb.removetarget(ipAddress="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_add_removed_target_again(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(container, lb, client)

    # remove the target
    lb = lb.removetarget(instanceId=container.id)
    validate_remove_target(container, lb, client)

    # add the target - should be allowed
    lb.addtarget(instanceId=container.id)


def test_destroy_container(client, context, config_id):
    container, lb = create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(container, lb, client)

    # destroy the instance
    # stop the lb instance
    container = client.wait_success(container)
    if container.state == 'running':
        container = client.wait_success(container.stop())
        assert container.state == 'stopped'

    # remove the lb instance
    container = client.wait_success(container.remove())
    assert container.state == 'removed'

    validate_remove_target(container, lb, client)


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
