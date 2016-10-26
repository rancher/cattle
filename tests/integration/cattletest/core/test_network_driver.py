from common_fixtures import *  # NOQA


def test_create_network_driver_create_delete(client, super_client):
    driver_name = 'test' + random_str()
    stack = client.create_stack(name=random_str())
    super_client.update(stack, system=True)
    s = client.create_network_driver_service(
        name=random_str(),
        stackId=stack.id,
        networkDriver={
            'name': driver_name,
        })

    s = client.wait_success(s)
    assert s.state == 'inactive'

    nds = client.list_network_driver(serviceId=s.id,
                                     name=driver_name)
    assert len(nds) == 1

    s = client.wait_success(s.activate())
    assert s.state == 'active'

    nd = find_one(client.list_network_driver, serviceId=s.id, name=driver_name)
    nd = client.wait_success(nd)

    assert nd.state == 'active'
    assert nd.serviceId == s.id

    s = client.wait_success(s.remove())
    assert s.state == 'removed'
    nd = client.wait_success(nd)
    assert nd.state == 'removed'


def test_create_network_driver_create_delete_with_net(client, super_client):
    driver_name = 'test' + random_str()
    stack = client.create_stack(name=random_str())
    super_client.update(stack, system=True)
    s = client.create_network_driver_service(
        name=random_str(),
        stackId=stack.id,
        selectorContainer='none',
        networkDriver={
            'name': driver_name,
            'defaultNetwork': {
                'subnets': [
                    {
                        'networkAddress': '1.1.1.1/8'
                    }
                ],
                'hostPorts': True,
                'dns': ['1.1.1.1'],
                'dnsSearch': ['domain.com',
                              'test.com'],
            },
            'networkMetadata': {
                'foo': 'bar',
                'bar': 2,
            },
            'cniConf': {
                'file.conf': {
                    'x': 'z',
                }
            }
        })

    s = client.wait_success(s)
    assert s.state == 'inactive'

    nds = client.list_network_driver(serviceId=s.id,
                                     name=driver_name)
    assert len(nds) == 1

    s = client.wait_success(s.activate())
    assert s.state == 'active'

    nd = find_one(client.list_network_driver, serviceId=s.id, name=driver_name)
    nd = client.wait_success(nd)

    assert nd.state == 'active'
    assert nd.serviceId == s.id

    network = find_one(nd.networks)
    network = client.wait_success(network)

    assert network.state == 'active'
    assert network.hostPorts

    subnet = find_one(super_client.list_subnet, networkId=network.id)
    subnet = super_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '1.1.1.1'
    assert subnet.cidrSize == 8
    assert subnet.gateway == '1.0.0.1'

    s = client.wait_success(s.remove())
    assert s.removed is not None
    nd = client.wait_success(nd)
    assert nd.removed is not None
    network = client.wait_success(network)
    assert network.removed is not None
    subnet = super_client.wait_success(subnet)
    assert subnet.removed is not None
