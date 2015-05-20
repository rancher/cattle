from common_fixtures import *  # NOQA
from cattle import ApiError


def test_lb_listener_create(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='8080',
                                                  targetPort='80',
                                                  sourceProtocol='http',
                                                  targetProtocol='tcp',
                                                  algorithm='leastconn')
    listener = client.wait_success(listener)

    assert listener.state == 'active'
    assert listener.sourcePort == 8080
    assert listener.sourceProtocol == 'http'
    assert listener.targetPort == 80
    assert listener.targetProtocol == 'tcp'
    assert listener.algorithm == 'leastconn'


def test_lb_listener_create_wo_algorithm(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='80',
                                                  sourceProtocol='tcp')
    listener = client.wait_success(listener)

    assert listener.algorithm == 'roundrobin'


def test_lb_listener_protocol_validation(client):
    # test for source protocol
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='8080',
                                           targetPort='80',
                                           sourceProtocol='udp',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'sourceProtocol'

    # test for target protocol
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='8080',
                                           targetPort='80',
                                           sourceProtocol='tcp',
                                           targetProtocol='udp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'targetProtocol'


def test_lb_listener_port_validation(client):
    # test maximum limit for source port
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='65536',
                                           targetPort='80',
                                           sourceProtocol='http',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLimitExceeded'
    assert e.value.error.fieldName == 'sourcePort'

    # test maximum limit for target port
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='80',
                                           targetPort='65537',
                                           sourceProtocol='http',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MaxLimitExceeded'
    assert e.value.error.fieldName == 'targetPort'

    # test minimum limit for source port
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='0',
                                           targetPort='80',
                                           sourceProtocol='http',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MinLimitExceeded'
    assert e.value.error.fieldName == 'sourcePort'

    # test minimum limit for target port
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='80',
                                           targetPort='-1',
                                           sourceProtocol='http',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MinLimitExceeded'
    assert e.value.error.fieldName == 'targetPort'


def test_lb_listener_delete(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='8080',
                                                  targetPort='80',
                                                  sourceProtocol='http',
                                                  targetProtocol='http')
    listener = client.wait_success(listener)

    listener = client.wait_success(client.delete(listener))

    assert listener.state == 'removed'


def test_lb_listener_update(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='8080',
                                                  targetPort='80',
                                                  sourceProtocol='http',
                                                  targetProtocol='http')
    listener = client.wait_success(listener)

    listener = client.update(listener, name='newName')
    assert listener.name == 'newName'


def test_lb_listener_create_wo_src_port(client):
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           targetPort='80',
                                           sourceProtocol='http',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'sourcePort'


def test_lb_listener_create_wo_src_protocol(client):
    with pytest.raises(ApiError) as e:
        client.create_loadBalancerListener(name=random_str(),
                                           sourcePort='80',
                                           targetPort='80',
                                           targetProtocol='tcp')

    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'
    assert e.value.error.fieldName == 'sourceProtocol'


def test_lb_listener_create_wo_target_portprotocol(client):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='80',
                                                  sourceProtocol='tcp')
    listener = client.wait_success(listener)
    assert listener.state == 'active'
    assert listener.targetPort == 80
    assert listener.targetProtocol == 'tcp'
