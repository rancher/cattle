from common_fixtures import *  # NOQA


def test_host_deactivate(super_client, new_context):
    host = new_context.host
    agent = super_client.reload(host).agent()

    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    host = super_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = super_client.wait_success(agent)
    assert agent.state == 'active'


def test_host_deactivate_two_hosts(super_client, new_context):
    host = new_context.host
    agent = super_client.reload(host).agent()

    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    # Create another host using the same agent
    other_host = super_client.create_host(agentId=agent.id)
    other_host = super_client.wait_success(other_host)
    assert other_host.state == 'active'
    assert other_host.agentId == agent.id

    host = super_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = super_client.wait_success(agent)
    assert agent.state == 'active'


def test_host_activate(super_client, new_context):
    host = new_context.host
    agent = super_client.reload(host).agent()

    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    host = super_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    host = super_client.wait_success(host.activate())
    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'


def test_host_purge(super_client, new_context):
    account_id = new_context.project.id
    image_uuid = 'sim:{}'.format(random_num())
    host = new_context.host
    phy_host = super_client.reload(host).physicalHost()
    agent = super_client.reload(host).agent()

    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    c1 = super_client.create_container(accountId=account_id,
                                       imageUuid=image_uuid,
                                       requestedHostId=host.id)
    c1 = super_client.wait_success(c1)
    assert c1.state == 'running'

    c2 = super_client.create_container(accountId=account_id,
                                       imageUuid=image_uuid,
                                       requestedHostId=host.id)
    c2 = super_client.wait_success(c2)
    assert c2.state == 'running'

    host = super_client.wait_success(host.deactivate())
    host = super_client.wait_success(super_client.delete(host))
    assert host.removed is not None

    agent = super_client.wait_success(host.agent())
    wait_for(lambda: super_client.reload(agent).state == 'removed')

    phy_host = super_client.wait_success(phy_host)
    assert phy_host.removed is not None

    c1 = super_client.wait_success(c1)
    assert c1.removed is not None

    c2 = super_client.wait_success(c2)
    assert c2.removed is not None

    volumes = c1.volumes()
    assert len(volumes) == 0


def test_host_container_actions_inactive(new_context):
    host = new_context.host
    client = new_context.client
    c = new_context.create_container()

    host = client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    c = client.wait_success(c.stop())
    assert c.state == 'stopped'

    c = client.wait_success(c.start())
    assert c.state == 'running'


def test_host_create_container_inactive(new_context):
    client = new_context.client
    host = new_context.host
    host = client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    c = new_context.create_container_no_success()
    assert c.transitioning == 'error'


def test_host_create_container_requested_inactive(super_client, new_context):
    client = new_context.client
    host = new_context.host
    host = client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    c = new_context.create_container_no_success(requestedHostId=host.id)
    wait_for(lambda: super_client.reload(c).transitioning == 'error')


def test_host_agent_state(super_client, new_context):
    agent = super_client.reload(new_context.host).agent()
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    host = new_context.client.reload(new_context.host)
    assert host.state == 'active'

    agent = super_client.wait_success(agent.deactivate())
    host = super_client.reload(host)
    assert agent.state == 'inactive'
    assert host.state == 'inactive'
    assert agent.state == host.agentState

    agent = super_client.wait_success(agent.activate())
    host = new_context.client.reload(new_context.host)

    assert host.state == 'active'
    assert agent.state == 'active'
    assert agent.state == host.agentState


def test_host_remove(super_client, new_context):
    client = new_context.client

    container = new_context.create_container()
    host = super_client.reload(new_context.host)
    pool = find_one(host.storagePools)
    agent = host.agent()
    agent_account = agent.account()
    phy_host = host.physicalHost()
    key = find_one(super_client.list_register, key=agent.data.registrationKey)
    instances = host.instances()
    assert len(instances) == 1

    assert container.state == 'running'
    assert host.state == 'active'
    assert pool.state == 'active'
    assert agent.state == 'active'
    assert agent_account.state == 'active'
    assert phy_host.state == 'active'
    assert key.state == 'active'
    assert key.secretKey is not None

    host = client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    host = client.wait_success(client.delete(host))
    assert host.removed is not None

    agent = super_client.wait_success(agent)
    wait_for(lambda: super_client.reload(agent).removed is not None)

    pool = super_client.wait_success(pool)
    assert pool.removed is not None

    phy_host = super_client.wait_success(phy_host)
    assert phy_host.removed is not None

    key = super_client.wait_success(key)
    assert key.removed is not None

    agent_account = super_client.wait_success(agent_account)
    assert agent_account.removed is not None

    container = super_client.wait_success(container)
    assert container.removed is not None

    for c in instances:
        c = super_client.wait_success(c)
        assert c.removed is not None


def test_host_dockersocket(context, client):
    host = client.reload(context.host)
    dockersocket = host.dockersocket()
    assert dockersocket.token.index('.') > 0
    assert '/v1/dockersocket/' in dockersocket.url


def test_host_dockersocket_inactive(context, client):
    host = client.wait_success(context.host.deactivate())
    dockersocket = host.dockersocket()
    assert dockersocket.token.index('.') > 0
    assert '/v1/dockersocket/' in dockersocket.url
