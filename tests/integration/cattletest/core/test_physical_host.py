from common_fixtures import *  # NOQA


def test_register_physical_host(super_client):
    uri = 'sim://{}'.format(random_str())
    scope = 'io.cattle.platform.process.agent.AgentActivate'
    agent = super_client.create_agent(uri=uri, data={
        scope: {'waitForPing': True}
    })

    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    wait_for(lambda: len(agent.hosts()) == 1)
    hosts = agent.hosts()

    assert len(hosts) == 1
    host = hosts[0]

    assert host.state == 'active'

    host = super_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    host = super_client.wait_success(host.remove())
    assert host.state == 'removed'

    agent = super_client.wait_success(agent)
    assert agent.state == 'removed'
