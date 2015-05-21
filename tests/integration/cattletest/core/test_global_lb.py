from common_fixtures import *  # NOQA
from cattle import ApiError


@pytest.fixture(scope='module')
def config_id(context):
    client = context.client
    default_lb_config = client.\
        create_loadBalancerConfig(name=random_str())
    default_lb_config = client.wait_success(default_lb_config)
    return default_lb_config.id


def create_glb_and_lb(client, config_id):
    # create global load balancer
    glb = client.create_globalLoadBalancer(name=random_str())
    glb = client.wait_success(glb)
    # create load balancer
    lb = create_valid_lb(client, config_id)
    return glb, lb


def create_valid_lb(client, config_id):
    default_lb_config = client.\
        create_loadBalancerConfig(name=random_str())
    client.wait_success(default_lb_config)

    test_lb = client.create_loadBalancer(name=random_str(),
                                         loadBalancerConfigId=config_id)
    test_lb = client.wait_success(test_lb)
    return test_lb


# test (C)
def test_global_lb_create(client):
    glb = client.create_globalLoadBalancer(name=random_str())
    glb = client.wait_success(glb)

    assert glb.state == 'active'


# test (D)
def test_global_lb_remove(client):
    # create global load balancer
    glb = client.create_globalLoadBalancer(name=random_str())
    glb = client.wait_success(glb)

    # delete newly created global load balancer
    glb = client.wait_success(client.delete(glb))
    glb = client.wait_success(glb)

    assert glb.state == "removed"


# test (U)
def test_global_lb_update(client):
    glb = client.create_globalLoadBalancer(name=random_str())
    glb = client.wait_success(glb)

    # update the lb
    glb = client.update(glb, name='newName')
    assert glb.name == 'newName'


def test_add_lb_to_glb(client, config_id):
    glb, lb = create_glb_and_lb(client, config_id)

    # add load balancer to global load balancer
    glb = glb.addloadbalancer(loadBalancerId=lb.id, weight=3)
    glb = client.wait_success(glb)

    lb = client.list_loadBalancer(name=lb.name)

    assert lb[0].weight == 3
    assert lb[0].globalLoadBalancerId == glb.id


def test_remove_lb_from_glb(client, config_id):
    glb, lb = create_glb_and_lb(client, config_id)

    # add lb to glb
    glb = glb.addloadbalancer(loadBalancerId=lb.id, weight=3)
    glb = client.wait_success(glb)

    # remove lb from glb
    glb = glb.removeloadbalancer(loadBalancerId=lb.id)
    glb = client.wait_success(glb)

    lb = client.list_loadBalancer(name=lb.name)

    assert lb[0].weight is None
    assert lb[0].globalLoadBalancerId is None


def test_add_lb_to_glb_no_weight(client, config_id):
    glb, lb = create_glb_and_lb(client, config_id)

    # add load balancer to global load balancer without the weight
    with pytest.raises(ApiError) as e:
        glb = glb.addloadbalancer(loadBalancerId=lb.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'weight'


def test_add_lb_to_glb_no_glb_id(client, config_id):
    glb, lb = create_glb_and_lb(client, config_id)

    # add load balancer to global load balancer without the weight
    with pytest.raises(ApiError) as e:
        glb = glb.addloadbalancer(weight=10)

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'loadBalancerId'
