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
def test_lb_create_w_config(client, config_id):
    lb = _create_valid_lb(client, config_id)

    assert lb.state == 'active'
    assert lb.loadBalancerConfigId == config_id


# test (D)
def test_lb_remove(client, config_id):
    # create lb
    lb = _create_valid_lb(client, config_id)

    # remove newly created lb
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'


# test (U)
def test_lb_update(client, config_id):
    # create lb
    lb = _create_valid_lb(client, config_id)

    # update the lb
    lb = client.update(lb, name='newName')
    assert lb.name == 'newName'


def test_lb_add_target_instance(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container=container)

    _validate_add_target(container, lb, client)


def test_lb_add_target_instance_with_ports(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container=container,
                     ports=["a.com:77", "b.com:99"])

    _validate_add_target(container, lb,
                         client, ports=["a.com:77", "b.com:99"])


def test_lb_remove_target_instance(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    lb = _add_target(lb, container=container)
    _validate_add_target(container, lb, client)

    # remove the target and verify that the target no longer exists
    lb = _remove_target(lb, container)
    _validate_remove_target(container, lb, client)


def test_lb_add_target_ip_address(client, context, config_id):
    lb = _create_valid_lb(client, config_id)
    ip_address = "10.1.1.1"
    lb = _add_target(lb, ip_address=ip_address)
    lb = client.wait_success(lb)

    _validate_add_target_ip(ip_address, lb, client)


def test_lb_remove_target_ip_address(client, context, config_id):
    lb = _create_valid_lb(client, config_id)

    # add target to a load balancer and verify that it got created
    ip_address = "10.1.1.1"
    lb = _add_target(lb, ip_address=ip_address)
    _validate_add_target_ip(ip_address, lb, client)

    # remove the target and verify that the target no longer exists
    lb = _remove_target(lb, ip_address=ip_address)
    _validate_remove_target_ip(ip_address, lb, client)


def test_lb_remove_w_target(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container=container)
    lb = client.wait_success(lb)

    # remove the load balancer
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'

    _validate_remove_target(container, lb, client)


def test_lb_remove_w_host(client, context, config_id):
    host = context.host
    # create lb, assign the hosts to it
    lb = _create_valid_lb(client, config_id)

    lb = lb.addhost(hostId=host.id)
    _validate_add_host(host, lb, client)

    # remove the load balancer
    lb = client.wait_success(client.delete(lb))
    assert lb.state == 'removed'

    _validate_remove_host(host, lb, client)


def test_set_target_instance(client, context, config_id):
    container1, lb = _create_lb_and_container(client, context, config_id)

    container2 = client. \
        create_container(imageUuid=context.image_uuid,
                         startOnCreate=False)
    container2 = client.wait_success(container2)

    # set 2 targets
    lb = _set_targets(lb, containers=[container1, container2])
    lb = client.wait_success(lb)

    _validate_add_target(container1, lb, client)

    _validate_add_target(container2, lb, client)

    # set 1 target
    lb = _set_targets(lb, containers=[container1])

    _validate_add_target(container1, lb, client)
    _validate_remove_target(container2, lb, client)

    # set 0 targets
    lb = _set_targets(lb, containers=[])

    _validate_remove_target(container1, lb, client)


def test_lb_set_target_ip_address(client, context, config_id):
    lb = _create_valid_lb(client, config_id)

    # set 2 targets
    lb = _set_targets(lb, ip_addresses=["10.1.1.1", "10.1.1.2"])

    _validate_add_target_ip("10.1.1.1", lb, client)
    _validate_add_target_ip("10.1.1.2", lb, client)

    # set 1 target
    lb = _set_targets(lb, ip_addresses=["10.1.1.1"])

    _validate_add_target_ip("10.1.1.1", lb, client)
    _validate_remove_target_ip("10.1.1.2", lb, client)

    # set 0 targets
    lb = _set_targets(lb, ip_addresses=[])

    _validate_remove_target_ip("10.1.1.1", lb, client)


def test_set_target_instance_and_ip(client, context, config_id):
    container1, lb = _create_lb_and_container(client, context, config_id)

    # set 2 targets - one ip and one instanceId
    lb = _set_targets(lb, containers=[container1], ip_addresses=["10.1.1.1"])

    _validate_add_target(container1, lb, client)

    _validate_add_target_ip("10.1.1.1", lb, client)


def test_lb_add_target_instance_twice(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container)
    _validate_add_target(container, lb, client)

    with pytest.raises(ApiError) as e:
        _add_target(lb, container)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_remove_non_existing_target_instance(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        _remove_target(lb, container)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_address_and_instance(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    with pytest.raises(ApiError) as e:
        _add_target(lb, container=container, ip_address="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_add_target_w_no_option(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    with pytest.raises(ApiError) as e:
        _add_target(lb)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'instanceId'


def test_lb_add_target_ip_twice(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, ip_address="10.1.1.1")
    _validate_add_target_ip("10.1.1.1", lb, client)

    with pytest.raises(ApiError) as e:
        _add_target(lb, ip_address="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'ipAddress'


def test_lb_remove_non_existing_target_ip(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)
    # remove non-existing target
    with pytest.raises(ApiError) as e:
        _remove_target(lb, ip_address="10.1.1.1")

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'ipAddress'


def test_add_removed_target_again(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container=container)
    _validate_add_target(container, lb, client)

    # remove the target
    lb = _remove_target(lb, container)
    _validate_remove_target(container, lb, client)

    # add the target - should be allowed
    _add_target(lb, container)


def test_destroy_container(client, context, config_id):
    container, lb = _create_lb_and_container(client, context, config_id)

    # add target to a load balancer
    lb = _add_target(lb, container=container)
    _validate_add_target(container, lb, client)

    # destroy the instance
    # stop the lb instance
    container = client.wait_success(container)
    if container.state == 'running':
        container = client.wait_success(container.stop())
        assert container.state == 'stopped'

    # remove the lb instance
    container = client.wait_success(container.remove())
    assert container.state == 'removed'

    _validate_remove_target(container, lb, client)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def _validate_add_host(host, lb, client):
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host.id


def _validate_remove_host(host, lb, client):
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        client, host_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    return host_map


def _add_target(lb, container=None, ip_address=None, ports=None):
    container_id = container.id if container else None
    port_domains = ports if ports else ["99:99"]
    target = {"instanceId": container_id,
              "ipAddress": ip_address, "ports": port_domains}
    lb = lb.addtarget(loadBalancerTarget=target)
    return lb


def _remove_target(lb, container=None, ip_address=None, ports=None):
    container_id = container.id if container else None
    port_domains = ports if ports else ["99:99"]
    target = {"instanceId": container_id,
              "ipAddress": ip_address, "ports": port_domains}
    lb = lb.removetarget(loadBalancerTarget=target)
    return lb


def _validate_add_target_ip(ip_address, lb, client):
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert target_map.ipAddress == ip_address


def _validate_remove_target_ip(ip_address, lb, client):
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress=ip_address)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _validate_add_target(container, lb, client, ports=None):
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)

    if ports:
        assert target_map.ports == ports


def _validate_remove_target(container, lb, client):
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        client, target_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)


def _create_valid_lb(client, config_id):
    test_lb = client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config_id)

    test_lb = client.wait_success(test_lb)
    return test_lb


def _create_lb_and_container(client, context, config_id):
    # create load balancer
    lb = _create_valid_lb(client, config_id)

    # create a container, no need to start it
    container = client.create_container(imageUuid=context.image_uuid,
                                        startOnCreate=False)
    container = client.wait_success(container)
    return container, lb


def _set_targets(lb, containers=None, ip_addresses=None):
    targets = []
    for container in containers or []:
        target = {"instanceId": container.id, "ports": "foo.com:100"}
        targets.append(target)

    for ip in ip_addresses or []:
        target = {"ipAddress": ip, "ports": "bar.com:100"}
        targets.append(target)

    lb = lb.settargets(loadBalancerTargets=targets)
    return lb
