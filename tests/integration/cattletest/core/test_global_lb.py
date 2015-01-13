from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def config_id(admin_client):
    default_lb_config = admin_client.\
        create_loadBalancerConfig(name=random_str())
    default_lb_config = admin_client.wait_success(default_lb_config)
    return default_lb_config.id


def create_glb_and_lb(admin_client, config_id):
    # create global load balancer
    glb = admin_client.create_globalLoadBalancer(name=random_str())
    glb = admin_client.wait_success(glb)
    # create load balancer
    lb = create_valid_lb(admin_client, config_id)
    return glb, lb


def create_valid_lb(admin_client, config_id):
    default_lb_config = admin_client.\
        create_loadBalancerConfig(name=random_str())
    default_lb_config = admin_client.wait_success(default_lb_config)

    test_lb = admin_client.create_loadBalancer(name=random_str(),
                                               loadBalancerConfigId=config_id)
    test_lb = admin_client.wait_success(test_lb)
    return test_lb


# test (C)
def test_global_lb_create(admin_client):
    glb = admin_client.create_globalLoadBalancer(name=random_str())
    glb = admin_client.wait_success(glb)

    assert glb.state == 'active'


# test (D)
def test_global_lb_remove(admin_client):
    # create global load balancer
    glb = admin_client.create_globalLoadBalancer(name=random_str())
    glb = admin_client.wait_success(glb)

    # delete newly created global load balancer
    glb = admin_client.wait_success(admin_client.delete(glb))
    glb = admin_client.wait_success(glb)

    assert glb.state == "removed"


# test (U)
def test_global_lb_update(admin_client):
    glb = admin_client.create_globalLoadBalancer(name=random_str())
    glb = admin_client.wait_success(glb)

    # update the lb
    glb = admin_client.update(glb, name='newName')
    assert glb.name == 'newName'


def test_add_lb_to_glb(admin_client, config_id):
    glb, lb = create_glb_and_lb(admin_client, config_id)

    # add load balancer to global load balancer
    glb = glb.addloadbalancer(loadBalancerId=lb.id, weight=3)
    glb = admin_client.wait_success(glb)

    lb = admin_client.list_loadBalancer(name=lb.name)

    assert lb[0].weight == 3
    assert lb[0].globalLoadBalancerId == glb.id


def test_remove_lb_from_glb(admin_client, config_id):
    glb, lb = create_glb_and_lb(admin_client, config_id)

    # add lb to glb
    glb = glb.addloadbalancer(loadBalancerId=lb.id, weight=3)
    glb = admin_client.wait_success(glb)

    # remove lb from glb
    glb = glb.removeloadbalancer(loadBalancerId=lb.id)
    glb = admin_client.wait_success(glb)

    lb = admin_client.list_loadBalancer(name=lb.name)

    assert lb[0].weight is None
    assert lb[0].globalLoadBalancerId is None


def test_add_lb_to_glb_no_weight(admin_client, config_id):
    glb, lb = create_glb_and_lb(admin_client, config_id)

    # add load balancer to global load balancer without the weight
    with pytest.raises(ApiError) as e:
        glb = glb.addloadbalancer(loadBalancerId=lb.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'weight'


def test_add_lb_to_glb_no_glb_id(admin_client, config_id):
    glb, lb = create_glb_and_lb(admin_client, config_id)

    # add load balancer to global load balancer without the weight
    with pytest.raises(ApiError) as e:
        glb = glb.addloadbalancer(weight=10)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerId'
