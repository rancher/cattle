from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def network(admin_client):
    return admin_client.wait_success(admin_client.create_network())


@pytest.fixture(scope='module')
def vnet(admin_client, network):
    vnet = admin_client.create_vnet(networkId=network.id,
                                    uri='bridge:///42')
    return admin_client.wait_success(vnet)


@pytest.fixture(scope='module')
def subnet(admin_client, network):
    subnet = admin_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='16',
                                        networkId=network.id)
    return admin_client.wait_success(subnet)


def test_network_create_defaults(admin_client):
    network = admin_client.create_network()

    assert network.state == 'registering'
    assert not network.isPublic

    network = admin_client.wait_success(network)

    assert network.state == 'active'


def test_network_create(admin_client):
    network = admin_client.create_network(isPublic=True)

    assert network.state == 'registering'
    assert network.isPublic

    network = admin_client.wait_success(network)
    assert network.state == 'active'

    return network


def test_subnet_create_off_network_address(admin_client, network):
    subnet = admin_client.create_subnet(networkAddress='192.168.0.42',
                                        cidrSize='24',
                                        networkId=network.id)
    subnet = admin_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.42'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'


def test_subnet_create(admin_client, network):
    assert_required_fields(admin_client.create_subnet,
                           networkAddress='192.168.0.0',
                           cidrSize='24',
                           networkId=network.id)

    subnet = admin_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='24',
                                        networkId=network.id)

    assert subnet.state == 'registering'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway is None
    assert subnet.startAddress is None
    assert subnet.endAddress is None
    assert subnet.networkId == network.id

    subnet = admin_client.wait_success(subnet)

    assert subnet.state == 'active'
    assert subnet.networkAddress == '192.168.0.0'
    assert subnet.cidrSize == 24
    assert subnet.gateway == '192.168.0.1'
    assert subnet.startAddress == '192.168.0.2'
    assert subnet.endAddress == '192.168.0.250'

    assert subnet.network().id == network.id


def test_vnet_create(admin_client, network):
    assert_required_fields(admin_client.create_vnet,
                           networkId=network.id,
                           uri='bridge:///42')

    vnet = admin_client.create_vnet(networkId=network.id,
                                    uri='bridge:///42')

    assert vnet.state == 'registering'
    vnet = admin_client.wait_success(vnet)

    assert vnet.state == 'active'
    assert vnet.networkId == network.id
    assert vnet.uri == 'bridge:///42'

    assert vnet.network().id == network.id


def test_subnet_vnet_map(admin_client, subnet, vnet):
    map = admin_client.create_subnet_vnet_map(subnetId=subnet.id,
                                              vnetId=vnet.id)

    assert map.state == 'registering'

    map = admin_client.wait_success(map)

    assert map.state == 'active'
    assert map.vnetId == vnet.id
    assert map.subnetId == subnet.id


def test_ip_address_create(admin_client, admin_account):
    ip_address = admin_client.create_ip_address()

    assert ip_address.state == 'registering'
    ip_address = admin_client.wait_success(ip_address)

    assert ip_address.state == 'inactive'
    assert ip_address.accountId == admin_account.id
    assert ip_address.address is None

    ip_address = admin_client.wait_success(ip_address.activate())

    assert ip_address.state == 'active'
    assert ip_address.address is None


def test_ip_address_create_from_subnet(admin_client, admin_account, subnet):
    ip_address = admin_client.create_ip_address(subnetId=subnet.id,
                                                networkId=42)

    assert ip_address.state == 'registering'
    ip_address = admin_client.wait_success(ip_address)

    assert ip_address.state == 'inactive'
    assert ip_address.accountId == admin_account.id
    assert ip_address.address is None
    assert ip_address.networkId is None
    assert ip_address.subnet().id == subnet.id

    ip_address = admin_client.wait_success(ip_address.activate())

    assert ip_address.state == 'active'
    assert ip_address.address is not None
    assert ip_address.address.startswith('192.168')
    assert ip_address.networkId == ip_address.subnet().networkId


def test_ip_address_create_no_address_available(admin_client, admin_account):
    network = admin_client.create_network()
    subnet = admin_client.create_subnet(networkAddress='192.168.0.0',
                                        cidrSize='16',
                                        networkId=network.id,
                                        startAddress='192.168.0.3',
                                        endAddress='192.168.0.5')
    subnet = admin_client.wait_success(subnet)
    assert subnet.state == 'active'

    ip_addresses = []
    ip_address_addresses = []
    for _ in range(3):
        ip_address = admin_client.create_ip_address(subnetId=subnet.id)
        ip_address = admin_client.wait_success(ip_address.activate())
        ip_addresses.append(ip_address)
        assert ip_address.address is not None
        ip_address_addresses.append(ip_address.address)

    assert len(ip_address_addresses) == 3
    assert '192.168.0.3' in ip_address_addresses
    assert '192.168.0.4' in ip_address_addresses
    assert '192.168.0.5' in ip_address_addresses

    ip_address = admin_client.create_ip_address(subnetId=subnet.id)
    ip_address = admin_client.wait_transitioning(ip_address.activate())

    assert ip_address.state == 'inactive'
    assert ip_address.transitioning == 'error'
    assert ip_address.transitioningMessage == \
        'Failed to allocate IP from subnet : IP allocation error'


def test_ip_address_defined(admin_client):
    ip = admin_client.create_ip_address(address='192.168.192.168')
    ip = admin_client.wait_success(ip)

    assert ip.address == '192.168.192.168'
    assert ip.state == 'inactive'

    ip = admin_client.wait_success(ip.activate())
    assert ip.address == '192.168.192.168'
    assert ip.state == 'active'

    ip = admin_client.wait_success(ip.deactivate())
    assert ip.address == '192.168.192.168'
    assert ip.state == 'inactive'
