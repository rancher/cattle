from common_fixtures import *  # NOQA


@pytest.fixture(scope='session')
def network(internal_test_client):
    return internal_test_client.wait_success(
        internal_test_client.create_network())


@pytest.fixture(scope='session')
def vnet(internal_test_client, network):
    vnet = internal_test_client.create_vnet(networkId=network.id,
                                            uri='bridge:///42')
    return internal_test_client.wait_success(vnet)


@pytest.fixture(scope='session')
def subnet(internal_test_client, network):
    subnet = internal_test_client.create_subnet(networkAddress='192.168.0.0',
                                                cidrSize='16',
                                                networkId=network.id)
    return internal_test_client.wait_success(subnet)


def test_network_create_defaults(internal_test_client):
    network = internal_test_client.create_network()

    assert network.state == 'registering'
    assert not network.isPublic

    network = internal_test_client.wait_success(network)

    assert network.state == 'active'


def test_network_create(internal_test_client):
    network = internal_test_client.create_network(isPublic=True)

    assert network.state == 'registering'
    assert network.isPublic

    network = internal_test_client.wait_success(network)
    assert network.state == 'active'
    assert network.macPrefix is not None
    assert len(network.macPrefix) == 8
    assert network.macPrefix.startswith('02:')

    return network


def test_network_purge(internal_test_client):
    network = internal_test_client.create_network(isPublic=True)
    network = internal_test_client.wait_success(network)
    assert network.state == 'active'
    assert network.macPrefix.startswith('02:')

    network = internal_test_client.wait_success(network.deactivate())
    assert network.state == 'inactive'

    network = internal_test_client.wait_success(
        internal_test_client.delete(network))
    assert network.state == 'removed'
    assert network.macPrefix.startswith('02:')

    prefix = network.macPrefix
    items = internal_test_client.list_resource_pool(item=prefix)
    assert len(items) == 1
    network = internal_test_client.wait_success(network.purge())
    assert network.state == 'purged'
    assert network.macPrefix is None

    items = internal_test_client.list_resource_pool(item=prefix)
    assert len(items) == 0

    return network


def test_subnet_create_off_network_address(internal_test_client, network):
    subnet = internal_test_client.create_subnet(networkAddress='192.168.0.42',
                                                cidrSize='24',
                                                networkId=network.id)
    subnet = internal_test_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.42'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'


def test_subnet_create(internal_test_client, network):
    assert_required_fields(internal_test_client.create_subnet,
                           networkAddress='192.168.0.0',
                           networkId=network.id)

    subnet = internal_test_client.create_subnet(networkAddress='192.168.0.0',
                                                cidrSize='24',
                                                networkId=network.id)

    assert subnet.state == 'registering'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway is None
    assert subnet.startAddress is None
    assert subnet.endAddress is None
    assert subnet.networkId == network.id

    subnet = internal_test_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'

    assert subnet.network().id == network.id


def test_vnet_create(internal_test_client, network):
    assert_required_fields(internal_test_client.create_vnet,
                           networkId=network.id,
                           uri='bridge:///42')

    vnet = internal_test_client.create_vnet(networkId=network.id,
                                            uri='bridge:///42')

    assert vnet.state == 'registering'
    vnet = internal_test_client.wait_success(vnet)

    assert vnet.state == 'active'
    assert vnet.networkId == network.id
    assert vnet.uri == 'bridge:///42'

    assert vnet.network().id == network.id


def test_subnet_vnet_map(internal_test_client, subnet, vnet):
    map = internal_test_client.create_subnet_vnet_map(subnetId=subnet.id,
                                                      vnetId=vnet.id)

    assert map.state == 'registering'

    map = internal_test_client.wait_success(map)

    assert map.state == 'active'
    assert map.vnetId == vnet.id
    assert map.subnetId == subnet.id


def test_ip_address_create(internal_test_client, internal_test_account):
    ip_address = internal_test_client.create_ip_address()

    assert ip_address.state == 'registering'
    ip_address = internal_test_client.wait_success(ip_address)

    assert ip_address.state == 'active'
    assert ip_address.accountId == internal_test_account.id
    assert ip_address.address is None


def test_ip_address_create_from_subnet(internal_test_client,
                                       internal_test_account, subnet):
    ip_address = internal_test_client.create_ip_address(subnetId=subnet.id,
                                                        networkId=42)

    assert ip_address.state == 'registering'
    assert ip_address.accountId == internal_test_account.id
    assert ip_address.address is None
    assert ip_address.networkId is None
    assert ip_address.subnet().id == subnet.id

    ip_address = internal_test_client.wait_success(ip_address)

    assert ip_address.state == 'active'
    assert ip_address.address is not None
    assert ip_address.address.startswith('192.168')
    assert ip_address.networkId == ip_address.subnet().networkId


def test_ip_address_create_no_address_available(internal_test_client,
                                                admin_account):
    network = internal_test_client.create_network()
    subnet = internal_test_client.create_subnet(networkAddress='192.168.0.0',
                                                cidrSize='16',
                                                networkId=network.id,
                                                startAddress='192.168.0.3',
                                                endAddress='192.168.0.5')
    subnet = internal_test_client.wait_success(subnet)
    assert subnet.state == 'active'

    ip_addresses = []
    ip_address_addresses = []
    for _ in range(3):
        ip_address = internal_test_client.create_ip_address(subnetId=subnet.id)
        ip_address = internal_test_client.wait_success(ip_address)
        ip_addresses.append(ip_address)
        assert ip_address.address is not None
        ip_address_addresses.append(ip_address.address)

    assert len(ip_address_addresses) == 3
    assert '192.168.0.3' in ip_address_addresses
    assert '192.168.0.4' in ip_address_addresses
    assert '192.168.0.5' in ip_address_addresses

    ip_address = internal_test_client.create_ip_address(subnetId=subnet.id)
    ip_address = internal_test_client.wait_transitioning(ip_address)

    assert ip_address.state == 'inactive'
    assert ip_address.transitioning == 'error'
    assert ip_address.transitioningMessage == \
        'Failed to allocate IP from subnet : IP allocation error'


def test_ip_address_defined(internal_test_client):
    ip = internal_test_client.create_ip_address(address='192.168.192.168')
    ip = internal_test_client.wait_success(ip)

    assert ip.address == '192.168.192.168'
    assert ip.state == 'active'

    ip = internal_test_client.wait_success(ip.deactivate())
    assert ip.address == '192.168.192.168'
    assert ip.state == 'inactive'
