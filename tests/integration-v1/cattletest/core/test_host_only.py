from common_fixtures import *  # NOQA


def test_host_only_no_subset(super_client, new_context):
    client = new_context.client
    account = new_context.project

    network = super_client.create_host_only_network(accountId=account.id,
                                                    hostVnetUri='docker:///',
                                                    isPublic=True)
    network = super_client.wait_success(network)
    assert network.state == 'active'

    assert client.by_id_network(network.id) is not None

    c = client.create_container(networkMode=None,
                                networkIds=[network.id],
                                imageUuid=new_context.image_uuid)
    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.primaryIpAddress is None
    c = super_client.reload(c)
    nics = c.nics()
    assert len(nics) == 1
    assert nics[0].network().id == network.id

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


def test_host_only_subnet(super_client, new_context):
    account = new_context.project
    client = new_context.client
    network = super_client.create_host_only_network(accountId=account.id,
                                                    hostVnetUri='docker:///',
                                                    isPublic=True)
    network = super_client.wait_success(network)
    assert network.state == 'active'

    subnet = super_client.create_subnet(accountId=account.id,
                                        networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = super_client.wait_success(subnet)
    assert subnet.state == 'active'

    assert client.by_id_network(network.id) is not None

    c = client.create_container(networkMode=None,
                                networkIds=[network.id],
                                imageUuid=new_context.image_uuid)

    c = client.wait_success(c)
    assert c.state == 'running'
    assert c.primaryIpAddress is not None
    assert c.primaryIpAddress.startswith('192.168.0')
    c = super_client.reload(c)
    nics = c.nics()
    assert len(nics) == 1
    assert nics[0].network().id == network.id

    c = super_client.reload(c)

    nic = c.nics()[0]

    assert nic.subnetId == subnet.id
    assert nic.networkId == network.id
    assert nic.vnetId is not None


def test_host_only_3_hosts_subnet(super_client, new_context):
    account = new_context.project
    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    host3 = register_simulated_host(new_context)

    network = super_client.create_host_only_network(accountId=account.id,
                                                    hostVnetUri='docker:///',
                                                    dynamicCreateVnet=True,
                                                    isPublic=True)

    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.dynamicCreateVnet

    subnet = super_client.create_subnet(accountId=account.id,
                                        networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = super_client.wait_success(subnet)
    assert subnet.state == 'active'

    assert super_client.by_id_network(network.id) is not None

    for host in [host1, host2, host3]:
        c = super_client.create_container(networkMode=None,
                                          accountId=account.id,
                                          networkIds=[network.id],
                                          requestedHostId=host.id,
                                          imageUuid=new_context.image_uuid)
        c = super_client.wait_success(c)
        assert c.state == 'running'

    assert len(network.vnets()) == 3
    for vnet in network.vnets():
        assert len(vnet.hosts()) == 1

    assert len(subnet.vnets()) == 3


# @pytest.mark.skipif('True')
def test_host_only_3_hosts_same_phy(super_client, new_context):
    account = new_context.project
    network = super_client.create_host_only_network(accountId=account.id,
                                                    hostVnetUri='docker:///',
                                                    dynamicCreateVnet=True)
    network = super_client.wait_success(network)
    assert network.state == 'active'
    assert network.dynamicCreateVnet

    subnet = super_client.create_subnet(accountId=account.id,
                                        networkId=network.id,
                                        networkAddress='192.168.0.0')
    subnet = super_client.wait_success(subnet)
    assert subnet.state == 'active'

    agents = new_context.agent_client.list_agent()
    assert len(agents) == 1
    agent = super_client.wait_success(agents[0])
    assert agent.state == 'active'
    # Deactivate so there is just one agent in this account
    agent.deactivate()

    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    pl_id = get_plain_id(super_client, account)
    agent = super_client.create_agent(accountId=new_context.agent.id,
                                      uri=uri,
                                      data={
                                          scope: {
                                              'hosts': 3
                                          },
                                          'agentResourcesAccountId': pl_id,
                                      })
    # This will hang right here because there are two agents in this
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    for _ in range(20):
        if len(agent.hosts()) == 3:
            break
        time.sleep(0.5)

    wait_for(lambda: len(agent.hosts()) == 3)
    assert len(agent.hosts()) == 3

    physical_host_id = agent.hosts()[0].physicalHostId
    for host in agent.hosts():
        host = super_client.wait_success(host)
        assert host.accountId == account.id
        assert host.state == 'active'
        assert host.physicalHostId == physical_host_id

        c = super_client.create_container(networkMode=None,
                                          accountId=account.id,
                                          requestedHostId=host.id,
                                          imageUuid=new_context.image_uuid,
                                          networkIds=[network.id])
        c = super_client.wait_success(c)
        assert c.state == 'running'
        assert len(c.nics()) == 1

    vnets = network.vnets()
    assert len(vnets) == 1
    assert len(vnets[0].hostVnetMaps()) == 3

    for map in vnets[0].hostVnetMaps():
        map = super_client.wait_success(map)
        assert map.state == 'active'
        assert map.host().physicalHostId == physical_host_id
