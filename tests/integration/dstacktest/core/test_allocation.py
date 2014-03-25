from common_fixtures import *  # NOQA


def test_compute_free(admin_client, sim_context):
    count = 5
    host = sim_context['host']
    image_uuid = sim_context['imageUuid']
    start_free = host.computeFree

    assert start_free > count

    containers = []
    for _ in range(count):
        c = admin_client.create_container(imageUuid=image_uuid,
                                          requestedHostId=host.id)
        containers.append(c)

    containers = wait_all_success(admin_client, containers)

    host = admin_client.reload(host)
    assert host.computeFree == start_free - count

    for c in containers:
        c.stop()

    containers = wait_all_success(admin_client, containers)

    host = admin_client.reload(host)
    assert host.computeFree == start_free


def _get_sim_hosts(admin_client):
    ret = []
    for h in admin_client.list_host(removed_null=True, kind='sim'):
        if h.agent().state == 'active':
            ret.append(h)

    return ret


def test_inactive_agent(admin_client, sim_context):
    agent = create_type_by_uuid(admin_client, 'agent', 'inactive_test',
                                uri='sim://inactive_test')

    agent = wait_success(admin_client, agent)
    host = agent.hosts()[0]

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)

    c = wait_success(admin_client, c)
    assert c.state == 'running'

    agent = wait_success(admin_client, agent.deactivate())
    assert agent.state == 'inactive'

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)

    c = wait_transitioning(admin_client, c)
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'
    assert c.state == 'removed'


def test_spread(admin_client, sim_context):
    count = 3
    num = random_num()

    hosts = _get_sim_hosts(admin_client)
    if len(hosts) < 3:
        host_count = 3
        agents = []
        for i in range(host_count):
            uri = 'sim://{}?{}'.format(num, i)
            agents.append(admin_client.create_agent(uri=uri))

        agents = wait_all_success(admin_client, agents)
        for agent in agents:
            while len(agent.hosts()) == 0 or len(agent.storagePools()) == 0:
                time.sleep(1)

    hosts = wait_all_success(admin_client, _get_sim_hosts(admin_client))
    for h in hosts:
        assert h.state == 'active'
        assert h.agent().state == 'active'
        assert len(h.agent().storagePools()) == 1
        assert h.agent().storagePools()[0].state == 'active'

    counts = []
    for i, h in enumerate(hosts):
        h = admin_client.update(h, {
            'computeFree': 10000000
        })
        counts.append(h.computeFree)

    containers = []
    for _ in range(len(hosts) * count):
        c = admin_client.create_container(imageUuid=sim_context['imageUuid'])
        containers.append(c)

    containers = wait_all_success(admin_client, containers, timeout=60)

    for i, h in enumerate(hosts):
        h = admin_client.reload(h)
        assert counts[i] - count == h.computeFree


def _get_test_allocation_host(admin_client):
    name = 'allocation_test'
    hosts = admin_client.list_host(name=name)

    if len(hosts) == 0:
        agent = wait_success(admin_client,
                             admin_client.create_agent(uri='sim://' + name))
        hosts = agent.hosts()

    host = wait_success(admin_client, hosts[0])
    host = admin_client.update(host, {
        'computeFree': 1,
        'name': name
    })

    return host


def test_allocation_failed_on_create(admin_client, sim_context):
    host = _get_test_allocation_host(admin_client)

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_success(admin_client, c)

    assert c.state == 'running'

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_transitioning(admin_client, c)

    assert c.state == 'removed'
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'

    assert c.allocationState == 'inactive'
    assert c.volumes()[0].state == 'removed'


def test_allocation_failed_on_start(admin_client, sim_context):
    host = _get_test_allocation_host(admin_client)

    c1 = c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                           requestedHostId=host.id)
    c = wait_success(admin_client, c)
    assert c.state == 'running'

    c = wait_success(admin_client, c.stop())
    assert c.state == 'stopped'

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_success(admin_client, c)
    assert c.state == 'running'

    c1 = wait_transitioning(admin_client, c1.start())
    assert c1.state == 'stopped'
    assert c1.transitioning == 'error'
    assert c1.transitioningMessage == 'Failed to find a placement'

    c = wait_success(admin_client, c.stop())
    assert c.state == 'stopped'

    c1 = wait_success(admin_client, c1.start())
    assert c1.state == 'running'
    assert c1.transitioning == 'no'
    assert c1.transitioningMessage is None
