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


def test_spread(admin_client, sim_context, sim_context2, sim_context3):
    count = 3

    hosts = [sim_context['host'], sim_context2['host'], sim_context3['host']]
    hosts = wait_all_success(admin_client, hosts)
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
        agent = create_type_by_uuid(admin_client, 'agent',
                                    'allocation_test_agent',
                                    uri='sim://' + name)
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


def test_host_vnet_association(admin_client, sim_context,
                               sim_context2, sim_context3):
    image_uuid = sim_context['imageUuid']
    host1 = sim_context['host']
    host2 = sim_context2['host']
    host3 = sim_context3['host']

    host1 = admin_client.update(host1, computeFree=100000)
    host2 = admin_client.update(host2, computeFree=100000)
    host3 = admin_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = admin_client.create_network()
    vnet = admin_client.create_vnet(networkId=network.id,
                                    uri='sim://')
    vnet = admin_client.wait_success(vnet)

    assert vnet.state == 'active'

    subnet1 = admin_client.create_subnet(networkAddress='192.168.0.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.0.3',
                                         endAddress='192.168.0.5')
    subnet1 = admin_client.wait_success(subnet1)

    subnet2 = admin_client.create_subnet(networkAddress='192.168.2.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.2.3',
                                         endAddress='192.168.3.5')
    subnet2 = admin_client.wait_success(subnet2)

    subnet_map1 = admin_client.create_subnet_vnet_map(subnetId=subnet1.id,
                                                      vnetId=vnet.id)
    subnet_map1 = admin_client.wait_success(subnet_map1)
    assert subnet_map1.state == 'active'

    subnet_map2 = admin_client.create_subnet_vnet_map(subnetId=subnet2.id,
                                                      vnetId=vnet.id)
    subnet_map2 = admin_client.wait_success(subnet_map2)
    assert subnet_map2.state == 'active'

    vnet_map1 = admin_client.create_host_vnet_map(hostId=host1.id,
                                                  vnetId=vnet.id)
    vnet_map1 = admin_client.wait_success(vnet_map1)
    assert vnet_map1.state == 'active'

    vnet_map2 = admin_client.create_host_vnet_map(hostId=host2.id,
                                                  vnetId=vnet.id)
    vnet_map2 = admin_client.wait_success(vnet_map2)
    assert vnet_map2.state == 'active'

    hosts = set()
    for _ in range(3):
        vm = admin_client.create_virtual_machine(subnetIds=[subnet1.id],
                                                 imageUuid=image_uuid)
        vm = admin_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    for _ in range(3):
        vm = admin_client.create_virtual_machine(subnetIds=[subnet2.id],
                                                 imageUuid=image_uuid)
        vm = admin_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    assert len(hosts) == 2
    assert host1.id in hosts
    assert host2.id in hosts
