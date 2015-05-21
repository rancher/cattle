from common_fixtures import *  # NOQA


@pytest.fixture(scope='module', autouse=True)
def disable_go_machine_service(request, super_client):
    # Ensuring the goMachineService handler is disabled lets us ensure these
    # tests are ran without actually calling docker machine and keeps them
    # lightweight. Reenables any disabled handlers at the end.
    name = "goMachineService"
    svc_handlers = super_client.list_external_handler(
        state='active', name_like=name)
    for h in svc_handlers:
        wait_success(super_client, h.deactivate())

    def enable(handlers, super_client):
        for handler in handlers:
            handler = super_client.reload(handler)
            try:
                wait_success(super_client, handler.activate())
            except AttributeError:
                pass

    request.addfinalizer(lambda: enable(svc_handlers, super_client))


def test_register_physical_host(super_client):
    uri = 'sim://{}'.format(random_str())
    scope = 'io.cattle.platform.process.agent.AgentActivate'
    agent = super_client.create_agent(uri=uri, data={
        scope: {'waitForPing': True}
    })

    agent = super_client.wait_success(agent)
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
    phys_host = hosts[0].physicalHost()
    assert phys_host is not None
    phys_host = super_client.wait_success(phys_host)
    assert phys_host.state == 'active'

    phys_host = super_client.wait_success(phys_host.remove())
    assert phys_host.state == 'removed'

    host = super_client.wait_success(super_client.reload(host))
    assert host.state == 'removed'


def test_register_multiple_physical_host(super_client):
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    scope2 = 'io.cattle.platform.process.agent.AgentActivate'
    uri = 'sim://{}'.format(random_str())
    agent = super_client.create_agent(uri=uri, data={
        scope: {
            'hosts': 2
        },
        scope2: {'waitForPing': True}
    })

    agent = super_client.wait_success(agent)
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

    phys_host = hosts[0].physicalHost()
    assert phys_host is not None
    phys_host = super_client.wait_success(phys_host)
    assert phys_host.state == 'active'

    phys_host = super_client.wait_success(phys_host.remove())
    assert phys_host.state == 'removed'

    for host in hosts:
        host = super_client.wait_success(super_client.reload(host))
        assert host.state == 'removed'


def test_add_physical_host(super_client):
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    scope2 = 'io.cattle.platform.process.agent.AgentActivate'
    uri = 'sim://{}'.format(random_str())
    agent = super_client.create_agent(uri=uri, data={
        scope: {
            'addPhysicalHost': False
        },
        scope2: {'waitForPing': True}
    })

    agent = super_client.wait_success(agent)
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
    agent = super_client.update(agent, {
        'data': agent.data
    })

    assert agent.data[scope]['addPhysicalHost']

    agent = super_client.wait_success(agent.reconnect())
    assert agent.state == 'active'

    hosts = agent.hosts()
    assert len(hosts) == 1
    assert hosts[0].physicalHostId is not None

    phys_host = hosts[0].physicalHost()
    assert phys_host is not None
    phys_host = super_client.wait_success(phys_host)
    assert phys_host.state == 'active'
