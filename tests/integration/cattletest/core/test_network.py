from common_fixtures import *  # NOQA


@pytest.fixture(scope='session')
def network(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    return super_client.wait_success(super_client.create_network(
        networkDriverId=nd.id))


@pytest.fixture(scope='session')
def vnet(super_client, network):
    vnet = super_client.create_vnet(networkId=network.id,
                                    uri='bridge:///42')
    return super_client.wait_success(vnet)


@pytest.fixture(scope='session')
def subnet(super_client, network):
    subnet = super_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='16',
                                        networkId=network.id)
    return super_client.wait_success(subnet)


def test_network_create_defaults(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(networkDriverId=nd.id)

    assert network.state == 'registering'
    assert not network.isPublic

    network = super_client.wait_success(network)

    assert network.state == 'active'


def test_network_create(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id)

    assert network.state == 'registering'
    assert network.isPublic

    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.macPrefix is not None
    assert len(network.macPrefix) == 8
    assert network.macPrefix.startswith('02:')

    return network


def test_network_purge(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(isPublic=True,
                                          networkDriverId=nd.id)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.macPrefix.startswith('02:')

    network = super_client.wait_success(network.deactivate())
    assert network.state == 'inactive'

    network = super_client.wait_success(super_client.delete(network))
    assert network.state == 'removed'
    assert network.macPrefix.startswith('02:')

    prefix = network.macPrefix
    items = super_client.list_resource_pool(item=prefix)
    assert len(items) == 1
    network = super_client.wait_success(network.purge())
    assert network.state == 'purged'
    assert network.macPrefix is None

    items = super_client.list_resource_pool(item=prefix)
    assert len(items) == 0

    return network


def test_subnet_create_off_network_address(super_client, network):
    subnet = super_client.create_subnet(networkAddress='192.168.0.42',
                                        cidrSize='24',
                                        networkId=network.id)
    subnet = super_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.42'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'


def test_subnet_create(super_client, network):
    assert_required_fields(super_client.create_subnet,
                           networkAddress='192.168.0.0',
                           networkId=network.id)

    subnet = super_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='24',
                                        networkId=network.id)

    assert subnet.state == 'registering'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway is None
    assert subnet.startAddress is None
    assert subnet.endAddress is None
    assert subnet.networkId == network.id

    subnet = super_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'

    assert subnet.network().id == network.id


def test_ip_address_create(super_client, super_account):
    ip_address = super_client.create_ip_address()

    assert ip_address.state == 'registering'
    ip_address = super_client.wait_success(ip_address)

    assert ip_address.state == 'active'
    assert ip_address.accountId == super_account.id
    assert ip_address.address is None


def test_ip_address_create_no_address_available(super_client):
    nd = super_client.wait_success(super_client.create_network_driver())
    network = super_client.create_network(networkDriverId=nd.id)
    subnet = super_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='16',
                                        networkId=network.id,
                                        startAddress='192.168.0.3',
                                        endAddress='192.168.0.5')
    subnet = super_client.wait_success(subnet)
    assert subnet.state == 'active'
    network = super_client.wait_success(network)
    assert network.state == 'active'

    ip_addresses = []
    ip_address_addresses = []
    for _ in range(3):
        ip_address = super_client.create_ip_address(networkId=network.id)
        ip_address = super_client.wait_success(ip_address)
        ip_addresses.append(ip_address)
        assert ip_address.address is not None
        ip_address_addresses.append(ip_address.address)

    assert len(ip_address_addresses) == 3
    assert '192.168.0.3' in ip_address_addresses
    assert '192.168.0.4' in ip_address_addresses
    assert '192.168.0.5' in ip_address_addresses

    ip_address = super_client.create_ip_address(networkId=network.id)
    ip_address = super_client.wait_transitioning(ip_address)

    assert ip_address.state == 'inactive'
    assert ip_address.transitioning == 'error'
    assert ip_address.transitioningMessage == \
        'Failed to allocate IP from subnet : IP allocation error'


def test_ip_address_defined(super_client):
    ip = super_client.create_ip_address(address='192.168.192.168')
    ip = super_client.wait_success(ip)

    assert ip.address == '192.168.192.168'
    assert ip.state == 'active'

    ip = super_client.wait_success(ip.deactivate())
    assert ip.address == '192.168.192.168'
    assert ip.state == 'inactive'
