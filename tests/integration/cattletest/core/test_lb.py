from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def config_id(admin_client):
    default_lb_config = admin_client. \
        create_loadBalancerConfig(name=random_str())
    default_lb_config = admin_client.wait_success(default_lb_config)
    return default_lb_config.id


# test (C)
def test_lb_create_wo_config(admin_client):
    with pytest.raises(ApiError) as e:
        admin_client.create_loadBalancer(name=random_str())

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerConfigId'


# test (C)
def create_valid_lb(admin_client, config_id):
    default_lb_config = admin_client.create_loadBalancerConfig(name='config')
    default_lb_config = admin_client.wait_success(default_lb_config)

    test_lb = admin_client.create_loadBalancer(name='test_lb',
                                               loadBalancerConfigId=config_id)
    test_lb = admin_client.wait_success(test_lb)
    return test_lb


def test_lb_create_w_config(admin_client, config_id):
    lb = create_valid_lb(admin_client, config_id)

    assert lb.state == 'active'
    assert lb.loadBalancerConfigId == config_id


# test (D)
def test_lb_remove(admin_client, config_id):
    # create lb
    lb = create_valid_lb(admin_client, config_id)

    # remove newly created lb
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'


# test (U)
def test_lb_update(admin_client, config_id):
    # create lb
    lb = create_valid_lb(admin_client, config_id)

    # update the lb
    lb = admin_client.update(lb, name='newName')
    assert lb.name == 'newName'


def test_lb_add_target_instance(admin_client, sim_context, config_id):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)

    assert len(target_map) == 1
    assert target_map[0].state == "active"
    assert target_map[0].instanceId == container.id


def test_lb_remove_target_instance(admin_client, sim_context, config_id):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id)

    lb = lb.addtarget(instanceId=container.id)
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)

    assert len(target_map) == 1

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(instanceId=container.id)
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)

    assert target_map[0].state == "removed"


def test_lb_add_target_ip_address(admin_client, sim_context, config_id):
    lb = create_valid_lb(admin_client, config_id)
    lb = lb.addtarget(ipAddress="10.1.1.1")
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1
    assert target_map[0].state == "active"
    assert target_map[0].ipAddress == "10.1.1.1"


def test_lb_remove_target_ip_address(admin_client, sim_context, config_id):
    lb = create_valid_lb(admin_client, config_id)

    # add target to a load balancer and verify that it got created
    lb = lb.addtarget(ipAddress="10.1.1.1")
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1

    # remove the target and verify that the target no longer exists
    lb = lb.removetarget(ipAddress="10.1.1.1")
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert target_map[0].state == "removed"


def create_lb_and_container(admin_client, sim_context, config_id):
    image_uuid = sim_context['imageUuid']
    # create load balancer
    lb = create_valid_lb(admin_client, config_id)
    # create a container, no need to start it
    container = admin_client.create_container(imageUuid=image_uuid,
                                              startOnCreate=False)
    container = admin_client.wait_success(container)
    return container, lb


def test_lb_remove_w_target(admin_client, sim_context, config_id):
    container, lb = create_lb_and_container(admin_client, sim_context,
                                            config_id)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=container.id)
    lb = admin_client.wait_success(lb)

    # remove the load balancer
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container.id)

    assert len(target_map) == 1
    assert target_map[0].state == "removed"


def test_lb_remove_w_host(admin_client, super_client, sim_context,
                          config_id):
    # create lb, assign the hosts to it
    host1 = super_client.create_host()
    host1 = super_client.wait_success(host1)

    lb = create_valid_lb(admin_client, config_id)

    lb = lb.addhost(hostId=host1.id)
    lb = admin_client.wait_success(lb)

    # remove the load balancer
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'

    host_map = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host1.id)

    assert len(host_map) == 1
    assert host_map[0].state == "removed"


def test_set_target_instance(admin_client, sim_context, config_id):
    container1, lb = create_lb_and_container(admin_client,
                                             sim_context, config_id)

    container2 = admin_client. \
        create_container(imageUuid=sim_context['imageUuid'],
                         startOnCreate=False)
    container2 = admin_client.wait_success(container2)

    # set 2 targets
    lb = lb.settargets(instanceIds=[container1.id, container2.id])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container2.id)

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    # set 1 target
    lb = lb.settargets(instanceIds=[container1.id])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container2.id)
    assert len(target_map) == 1
    assert target_map[0].state == "removed"

    # set 0 targets
    lb = lb.settargets(instanceIds=[])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)

    assert len(target_map) == 1
    assert target_map[0].state == "removed"


def test_lb_set_target_ip_address(admin_client, sim_context, config_id):
    lb = create_valid_lb(admin_client, config_id)

    # set 2 targets
    lb = lb.settargets(ipAddresses=["10.1.1.1", "10.1.1.2"])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.2")

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    # set 1 target
    lb = lb.settargets(ipAddresses=["10.1.1.1"])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.2")

    assert len(target_map) == 1
    assert target_map[0].state == "removed"

    # set 0 targets
    lb = lb.settargets(ipAddresses=[])
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1
    assert target_map[0].state == "removed"


def test_set_target_instance_and_ip(admin_client, sim_context, config_id):
    container1, lb = create_lb_and_container(admin_client, sim_context,
                                             config_id)

    # set 2 targets - one ip and one instanceId
    lb = lb.settargets(instanceIds=[container1.id],
                       ipAddresses="10.1.1.1")
    lb = admin_client.wait_success(lb)

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)

    assert len(target_map) == 1
    assert target_map[0].state == "active"

    target_map = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                ipAddress="10.1.1.1")

    assert len(target_map) == 1
    assert target_map[0].state == "active"
