from common_fixtures import *  # NOQA


def test_network_policy_default_action(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.defaultPolicyAction == 'allow'

    return network


def test_default_no_network_policy(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.policy is None

    return network


def test_default_network_policy(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())

    policy = ['{"within": "stack", "action": "allow"}']
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id,
                                          defaultPolicyAction='deny',
                                          policy=policy)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.defaultPolicyAction == 'deny'
    assert len(network.policy) == 1

    return network


def test_update_network_policy(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.defaultPolicyAction == 'allow'
    assert network.policy is None
    network = super_client.update(network, defaultPolicyAction='deny')
    assert network.defaultPolicyAction == 'deny'

    policy = ['{"within": "stack", "action": "allow"}']
    network = super_client.update(network, policy=policy)
    assert len(network.policy) == 1

    return network
