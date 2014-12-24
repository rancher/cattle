from common_fixtures import *  # NOQA


def test_compute_free(admin_client, internal_test_client, sim_context):
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

    host = internal_test_client.reload(host)
    assert host.computeFree == start_free - count

    for c in containers:
        c.stop(deallocateFromHost=True)

    containers = wait_all_success(admin_client, containers)

    host = internal_test_client.reload(host)
    assert host.computeFree == start_free


def test_inactive_agent(internal_test_client, sim_context):
    agent = create_type_by_uuid(internal_test_client, 'agent', 'inactive_test',
                                uri='sim://inactive_test')

    agent = wait_success(internal_test_client, agent)
    host = agent.hosts()[0]

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)

    c = wait_success(internal_test_client, c)
    assert c.state == 'running'

    agent = wait_success(internal_test_client, agent.deactivate())
    assert agent.state == 'inactive'

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)

    c = wait_transitioning(internal_test_client, c)
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'
    assert c.state == 'removed'


def test_spread(internal_test_client, sim_context, sim_context2, sim_context3):
    count = 3

    hosts = [sim_context['host'], sim_context2['host'], sim_context3['host']]
    hosts = wait_all_success(internal_test_client, hosts)
    for h in hosts:
        assert h.state == 'active'
        assert h.agent().state == 'active'
        assert len(h.agent().storagePools()) == 1
        assert h.agent().storagePools()[0].state == 'active'

    counts = []
    for i, h in enumerate(hosts):
        h = internal_test_client.update(h, {
            'computeFree': 10000000
        })
        counts.append(h.computeFree)

    containers = []
    for _ in range(len(hosts) * count):
        c = internal_test_client.create_container(
            imageUuid=sim_context['imageUuid'])
        containers.append(c)

    containers = wait_all_success(internal_test_client, containers, timeout=60)

    for i, h in enumerate(hosts):
        h = internal_test_client.reload(h)
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


def test_allocation_failed_on_create(internal_test_client, sim_context):
    host = _get_test_allocation_host(internal_test_client)

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)
    c = wait_success(internal_test_client, c)

    assert c.state == 'running'

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)
    c = wait_transitioning(internal_test_client, c)

    assert c.state == 'removed'
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'

    assert c.allocationState == 'inactive'
    assert c.volumes()[0].state == 'removed'


def test_allocation_failed_on_start(internal_test_client, sim_context):
    host = _get_test_allocation_host(internal_test_client)

    c1 = c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)
    c = wait_success(internal_test_client, c)
    assert c.state == 'running'

    c = wait_success(internal_test_client, c.stop(deallocateFromHost=True))
    assert c.state == 'stopped'

    c = internal_test_client.create_container(
        imageUuid=sim_context['imageUuid'], requestedHostId=host.id)
    c = wait_success(internal_test_client, c)
    assert c.state == 'running'

    c1 = wait_transitioning(internal_test_client, c1.start())
    assert c1.state == 'stopped'
    assert c1.transitioning == 'error'
    assert c1.transitioningMessage == 'Failed to find a placement'

    c = wait_success(internal_test_client, c.stop(deallocateFromHost=True))
    assert c.state == 'stopped'

    c1 = wait_success(internal_test_client, c1.start())
    assert c1.state == 'running'
    assert c1.transitioning == 'no'
    assert c1.transitioningMessage is None


def test_host_vnet_association(internal_test_client, sim_context,
                               sim_context2, sim_context3):
    image_uuid = sim_context['imageUuid']
    host1 = sim_context['host']
    host2 = sim_context2['host']
    host3 = sim_context3['host']

    host1 = internal_test_client.update(host1, computeFree=100000)
    host2 = internal_test_client.update(host2, computeFree=100000)
    host3 = internal_test_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = internal_test_client.create_network()
    vnet = internal_test_client.create_vnet(networkId=network.id,
                                            uri='sim://')
    vnet = internal_test_client.wait_success(vnet)

    assert vnet.state == 'active'

    subnet1 = internal_test_client.create_subnet(networkAddress='192.168.0.0',
                                                 cidrSize='16',
                                                 networkId=network.id,
                                                 startAddress='192.168.0.3',
                                                 endAddress='192.168.0.5')
    subnet1 = internal_test_client.wait_success(subnet1)

    subnet2 = internal_test_client.create_subnet(networkAddress='192.168.2.0',
                                                 cidrSize='16',
                                                 networkId=network.id,
                                                 startAddress='192.168.2.3',
                                                 endAddress='192.168.3.5')
    subnet2 = internal_test_client.wait_success(subnet2)

    subnet_map1 = internal_test_client.create_subnet_vnet_map(
        subnetId=subnet1.id, vnetId=vnet.id)
    subnet_map1 = internal_test_client.wait_success(subnet_map1)
    assert subnet_map1.state == 'active'

    subnet_map2 = internal_test_client.create_subnet_vnet_map(
        subnetId=subnet2.id, vnetId=vnet.id)
    subnet_map2 = internal_test_client.wait_success(subnet_map2)
    assert subnet_map2.state == 'active'

    vnet_map1 = internal_test_client.create_host_vnet_map(hostId=host1.id,
                                                          vnetId=vnet.id)
    vnet_map1 = internal_test_client.wait_success(vnet_map1)
    assert vnet_map1.state == 'active'

    vnet_map2 = internal_test_client.create_host_vnet_map(hostId=host2.id,
                                                          vnetId=vnet.id)
    vnet_map2 = internal_test_client.wait_success(vnet_map2)
    assert vnet_map2.state == 'active'

    hosts = set()
    for _ in range(3):
        vm = internal_test_client.create_virtual_machine(
            subnetIds=[subnet1.id],
            imageUuid=image_uuid)
        vm = internal_test_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    for _ in range(3):
        vm = internal_test_client.create_virtual_machine(
            subnetIds=[subnet2.id],
            imageUuid=image_uuid)
        vm = internal_test_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    assert len(hosts) == 2
    assert host1.id in hosts
    assert host2.id in hosts


def test_allocation_stay_associated_to_host(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'])
    c = admin_client.wait_success(c)

    assert c.state == 'running'

    c = admin_client.wait_success(c.stop())
    assert c.state == 'stopped'

    assert len(c.hosts()) == 1


def test_vnet_stickiness(internal_test_client, sim_context, sim_context2,
                         sim_context3):
    image_uuid = sim_context['imageUuid']
    host1 = sim_context['host']
    host2 = sim_context2['host']
    host3 = sim_context3['host']
    valid_hosts = [host1.id, host2.id, host3.id]

    host1 = internal_test_client.update(host1, computeFree=100000)
    host2 = internal_test_client.update(host2, computeFree=100000)
    host3 = internal_test_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = create_and_activate(internal_test_client, 'hostOnlyNetwork',
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True)

    subnet = create_and_activate(internal_test_client, 'subnet',
                                 networkAddress='192.168.0.0',
                                 networkId=network.id)

    containers = []
    for _ in range(3):
        c = internal_test_client.create_container(imageUuid=image_uuid,
                                                  networkIds=[network.id],
                                                  validHostIds=valid_hosts)
        c = internal_test_client.wait_success(c)
        containers.append(c)

    actual_hosts = set()
    for i in containers:
        assert i.state == 'running'
        actual_hosts.add(i.hosts()[0].id)

    assert actual_hosts == set(valid_hosts)
    assert len(network.vnets()) == 3
    assert len(subnet.vnets()) == 3

    c1_host_id = c.hosts()[0].id
    c1_nic = c.nics()[0]

    for _ in range(3):
        c = internal_test_client.create_container(imageUuid=image_uuid,
                                                  vnetIds=[c1_nic.vnetId])
        c = internal_test_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId

    for _ in range(3):
        c = internal_test_client.create_container(imageUuid=image_uuid,
                                                  networkIds=[network.id],
                                                  vnetIds=[c1_nic.vnetId])
        c = internal_test_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId
