from common_fixtures import *  # NOQA


def test_host_only_no_subset(admin_client, client, sim_context):
    network = admin_client.create_host_only_network(hostVnetUri='docker:///',
                                                    isPublic=True)
    network = admin_client.wait_success(network)
    assert network.state == 'active'

    assert client.by_id_network(network.id) is not None

    c = client.create_container(networkIds=[network.id],
                                imageUuid=sim_context['imageUuid'])
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.primaryIpAddress is None

    nics = c.nics()
    assert len(nics) == 1
    assert nics[0].network().id == network.id

    c = admin_client.reload(c)

    nic = c.nics()[0]

    assert nic.subnetId is None
    assert nic.networkId == network.id
    assert nic.vnetId is not None

    vnets = network.vnets()
    assert len(vnets) == 1

    assert vnets[0].id == nic.vnetId

    vnet = vnets[0]

    assert len(vnet.hosts()) == 1
    assert len(c.hosts()) == 1
    assert len(vnet.hostVnetMaps()) == 1
    assert vnet.hostVnetMaps()[0].state == 'active'
    assert c.hosts()[0].id == vnet.hosts()[0].id


def test_host_only_subnet(admin_client, client, sim_context):
    network = admin_client.create_host_only_network(hostVnetUri='docker:///',
                                                    isPublic=True)
    network = admin_client.wait_success(network)
    assert network.state == 'active'

    subnet = admin_client.create_subnet(networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = admin_client.wait_success(subnet)
    assert subnet.state == 'active'

    assert client.by_id_network(network.id) is not None

    c = client.create_container(networkIds=[network.id],
                                imageUuid=sim_context['imageUuid'])
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.primaryIpAddress is not None
    assert c.primaryIpAddress.startswith('192.168.0')

    nics = c.nics()
    assert len(nics) == 1
    assert nics[0].network().id == network.id

    c = admin_client.reload(c)

    nic = c.nics()[0]

    assert nic.subnetId == subnet.id
    assert nic.networkId == network.id
    assert nic.vnetId is not None


def test_host_only_3_hosts_subnet(admin_client, sim_context, sim_context2,
                                  sim_context3):
    network = admin_client.create_host_only_network(hostVnetUri='docker:///',
                                                    dynamicCreateVnet=True,
                                                    isPublic=True)
    network = admin_client.wait_success(network)
    assert network.state == 'active'
    assert network.dynamicCreateVnet

    subnet = admin_client.create_subnet(networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = admin_client.wait_success(subnet)
    assert subnet.state == 'active'

    assert admin_client.by_id_network(network.id) is not None

    for i in [sim_context, sim_context2, sim_context3]:
        host = i['host']
        c = admin_client.create_container(networkIds=[network.id],
                                          requestedHostId=host.id,
                                          imageUuid=sim_context['imageUuid'])
        c = admin_client.wait_success(c)
        assert c.state == 'running'

    assert len(network.vnets()) == 3
    for vnet in network.vnets():
        assert len(vnet.hosts()) == 1

    assert len(subnet.vnets()) == 3


def test_host_only_3_hosts_same_phy(admin_client, sim_context):
    network = admin_client.create_host_only_network(hostVnetUri='docker:///',
                                                    dynamicCreateVnet=True,
                                                    isPublic=True)
    network = admin_client.wait_success(network)
    assert network.state == 'active'
    assert network.dynamicCreateVnet

    subnet = admin_client.create_subnet(networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = admin_client.wait_success(subnet)
    assert subnet.state == 'active'

    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    agent = admin_client.create_agent(uri=uri, data={
        scope: {
            'hosts': 3
        }
    })
    agent = admin_client.wait_success(agent)
    assert agent.state == 'active'

    for _ in range(20):
        if len(agent.hosts()) == 3:
            break
        time.sleep(0.5)

    assert len(agent.hosts()) == 3

    physical_host_id = agent.hosts()[0].physicalHostId
    for host in agent.hosts():
        host = admin_client.wait_success(host)
        assert host.state == 'active'
        assert host.physicalHostId == physical_host_id

        c = admin_client.create_container(requestedHostId=host.id,
                                          imageUuid=sim_context['imageUuid'],
                                          networkIds=[network.id])
        c = admin_client.wait_success(c)
        assert c.state == 'running'
        assert len(c.nics()) == 1


    vnets = network.vnets()
    assert len(vnets) == 1
    assert len(vnets[0].hostVnetMaps()) == 3

    for map in vnets[0].hostVnetMaps():
        map = admin_client.wait_success(map)
        assert map.state == 'active'
        assert map.host().physicalHostId == physical_host_id
