from common_fixtures import *  # NOQA


def test_register_physical_host(internal_test_client):
    uri = 'sim://{}'.format(random_str())
    agent = internal_test_client.create_agent(uri=uri)

    agent = internal_test_client.wait_success(agent)
    assert agent.state == 'active'

    hosts = agent.hosts()

    for _ in range(10):
        hosts = agent.hosts()
        if len(hosts) == 0:
            time.sleep(0.5)
        else:
            break

    assert len(hosts) == 1
    host = hosts[0]

    assert host.physicalHostId is not None
    assert hosts[0].physicalHost() is not None


def test_register_multiple_physical_host(internal_test_client):
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    agent = internal_test_client.create_agent(uri=uri, data={
        scope: {
            'hosts': 2
        }
    })

    agent = internal_test_client.wait_success(agent)
    assert agent.state == 'active'

    hosts = agent.hosts()

    for _ in range(10):
        hosts = agent.hosts()
        if len(hosts) == 0:
            time.sleep(0.5)
        else:
            break

    assert len(hosts) == 2
    host1 = hosts[0]
    host2 = hosts[1]

    assert host1.physicalHostId is not None
    assert host2.physicalHostId is not None

    assert host1.physicalHostId == host2.physicalHostId


def test_add_physical_host(internal_test_client):
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    agent = internal_test_client.create_agent(uri=uri, data={
        scope: {
            'addPhysicalHost': False
        }
    })

    agent = internal_test_client.wait_success(agent)
    assert agent.state == 'active'

    hosts = agent.hosts()

    for _ in range(10):
        hosts = agent.hosts()
        if len(hosts) == 0:
            time.sleep(0.5)
        else:
            break

    assert len(hosts) == 1
    host1 = hosts[0]

    assert host1.physicalHostId is None

    agent.data[scope]['addPhysicalHost'] = True
    agent = internal_test_client.update(agent, {
        'data': agent.data
    })

    assert agent.data[scope]['addPhysicalHost']

    agent = internal_test_client.wait_success(agent.reconnect())
    assert agent.state == 'active'

    hosts = agent.hosts()
    assert len(hosts) == 1
    assert hosts[0].physicalHostId is not None
    assert hosts[0].physicalHost() is not None
