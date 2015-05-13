from common_fixtures import *  # NOQA


def test_compute_free(admin_client, super_client, sim_context):
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

    containers = wait_all_success(super_client, containers)

    host = super_client.reload(host)
    assert host.computeFree == start_free - count

    for c in containers:
        c.stop(deallocateFromHost=True)

    containers = wait_all_success(admin_client, containers)

    host = super_client.reload(host)
    assert host.computeFree == start_free


def test_inactive_agent(super_client, sim_context):
    agent = create_type_by_uuid(super_client, 'agent', 'inactive_test',
                                uri='sim://inactive_test')

    agent = wait_success(super_client, agent)
    host = agent.hosts()[0]

    c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)

    c = wait_success(super_client, c)
    assert c.state == 'running'

    agent = wait_success(super_client, agent.deactivate())
    assert agent.state == 'inactive'

    c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)

    c = wait_transitioning(super_client, c)
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'
    assert c.state == 'removed'


def test_spread(super_client, sim_context, sim_context2, sim_context3):
    count = 3

    hosts = [sim_context['host'], sim_context2['host'], sim_context3['host']]
    hosts = wait_all_success(super_client, hosts)
    for h in hosts:
        assert h.state == 'active'
        assert h.agent().state == 'active'
        assert len(h.agent().storagePools()) == 1
        assert h.agent().storagePools()[0].state == 'active'

    counts = []
    for i, h in enumerate(hosts):
        h = super_client.update(h, {
            'computeFree': 10000000
        })
        counts.append(h.computeFree)

    containers = []
    for _ in range(len(hosts) * count):
        c = super_client.create_container(imageUuid=sim_context['imageUuid'])
        containers.append(c)

    containers = wait_all_success(super_client, containers, timeout=60)

    for i, h in enumerate(hosts):
        h = super_client.reload(h)
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


def test_allocation_failed_on_create(super_client, sim_context):
    host = _get_test_allocation_host(super_client)

    c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_success(super_client, c)

    assert c.state == 'running'

    c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_transitioning(super_client, c)

    assert c.state == 'removed'
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find a placement'

    assert c.allocationState == 'inactive'
    assert c.volumes()[0].state == 'removed'


def test_allocation_failed_on_start(super_client, sim_context):
    host = _get_test_allocation_host(super_client)

    c1 = c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                           requestedHostId=host.id)
    c = wait_success(super_client, c)
    assert c.state == 'running'

    c = wait_success(super_client, c.stop(deallocateFromHost=True))
    assert c.state == 'stopped'

    c = super_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=host.id)
    c = wait_success(super_client, c)
    assert c.state == 'running'

    c1 = wait_transitioning(super_client, c1.start())
    assert c1.state == 'stopped'
    assert c1.transitioning == 'error'
    assert c1.transitioningMessage == 'Failed to find a placement'

    c = wait_success(super_client, c.stop(deallocateFromHost=True))
    assert c.state == 'stopped'

    c1 = wait_success(super_client, c1.start())
    assert c1.state == 'running'
    assert c1.transitioning == 'no'
    assert c1.transitioningMessage is None


def test_host_vnet_association(super_client, sim_context,
                               sim_context2, sim_context3):
    image_uuid = sim_context['imageUuid']
    host1 = sim_context['host']
    host2 = sim_context2['host']
    host3 = sim_context3['host']

    host1 = super_client.update(host1, computeFree=100000)
    host2 = super_client.update(host2, computeFree=100000)
    host3 = super_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = super_client.create_network()
    vnet = super_client.create_vnet(networkId=network.id,
                                    uri='sim://')
    vnet = super_client.wait_success(vnet)

    assert vnet.state == 'active'

    subnet1 = super_client.create_subnet(networkAddress='192.168.0.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.0.3',
                                         endAddress='192.168.0.5')
    subnet1 = super_client.wait_success(subnet1)

    subnet2 = super_client.create_subnet(networkAddress='192.168.2.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.2.3',
                                         endAddress='192.168.3.5')
    subnet2 = super_client.wait_success(subnet2)

    subnet_map1 = super_client.create_subnet_vnet_map(subnetId=subnet1.id,
                                                      vnetId=vnet.id)
    subnet_map1 = super_client.wait_success(subnet_map1)
    assert subnet_map1.state == 'active'

    subnet_map2 = super_client.create_subnet_vnet_map(subnetId=subnet2.id,
                                                      vnetId=vnet.id)
    subnet_map2 = super_client.wait_success(subnet_map2)
    assert subnet_map2.state == 'active'

    vnet_map1 = super_client.create_host_vnet_map(hostId=host1.id,
                                                  vnetId=vnet.id)
    vnet_map1 = super_client.wait_success(vnet_map1)
    assert vnet_map1.state == 'active'

    vnet_map2 = super_client.create_host_vnet_map(hostId=host2.id,
                                                  vnetId=vnet.id)
    vnet_map2 = super_client.wait_success(vnet_map2)
    assert vnet_map2.state == 'active'

    hosts = set()
    for _ in range(3):
        vm = super_client.create_virtual_machine(subnetIds=[subnet1.id],
                                                 imageUuid=image_uuid)
        vm = super_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    for _ in range(3):
        vm = super_client.create_virtual_machine(subnetIds=[subnet2.id],
                                                 imageUuid=image_uuid)
        vm = super_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    assert len(hosts) == 2
    assert host1.id in hosts
    assert host2.id in hosts


def test_allocation_stay_associated_to_host(super_client, sim_context):
    c = super_client.create_container(imageUuid=sim_context['imageUuid'])
    c = super_client.wait_success(c)

    assert c.state == 'running'

    c = super_client.wait_success(c.stop())
    assert c.state == 'stopped'

    assert len(c.hosts()) == 1


def test_vnet_stickiness(super_client, sim_context, sim_context2,
                         sim_context3):
    image_uuid = sim_context['imageUuid']
    host1 = sim_context['host']
    host2 = sim_context2['host']
    host3 = sim_context3['host']
    valid_hosts = [host1.id, host2.id, host3.id]

    host1 = super_client.update(host1, computeFree=100000)
    host2 = super_client.update(host2, computeFree=100000)
    host3 = super_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = create_and_activate(super_client, 'hostOnlyNetwork',
                                  hostVnetUri='test:///',
                                  dynamicCreateVnet=True)

    subnet = create_and_activate(super_client, 'subnet',
                                 networkAddress='192.168.0.0',
                                 networkId=network.id)

    containers = []
    for _ in range(3):
        c = super_client.create_container(imageUuid=image_uuid,
                                          networkIds=[network.id],
                                          validHostIds=valid_hosts)
        c = super_client.wait_success(c)
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
        c = super_client.create_container(imageUuid=image_uuid,
                                          vnetIds=[c1_nic.vnetId])
        c = super_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId

    for _ in range(3):
        c = super_client.create_container(imageUuid=image_uuid,
                                          networkIds=[network.id],
                                          vnetIds=[c1_nic.vnetId])
        c = super_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId


def test_port_constraint(super_client, sim_context, sim_context2, network):
    image_uuid = sim_context['imageUuid']

    host1 = sim_context['host']
    host2 = sim_context2['host']

    containers = []

    try:
        c = super_client.create_container(imageUuid=image_uuid,
                                          networkIds=[network.id],
                                          startOnCreate=True,
                                          validHostIds=[host1.id],
                                          ports=[
                                              '8081:81/tcp'])
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        containers.append(c)

        # try to deploy another container with same public port + protocol
        c2 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           validHostIds=[host1.id],
                                           startOnCreate=True,
                                           ports=[
                                               '8081:81/tcp'])
        c2 = wait_transitioning(super_client, c2)
        assert c2.transitioning == 'error'
        assert c2.transitioningMessage == 'Failed to find a placement'
        assert c2.state == 'removed'

        # increase host pool and check whether allocator picks other host
        c2 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           startOnCreate=True,
                                           validHostIds=[
                                               host1.id, host2.id],
                                           ports=['8081:81/tcp'])
        wait_for_condition(
            super_client, c2,
            lambda x: x.state == 'running')
        containers.append(c2)

        # try different public port
        c3 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           startOnCreate=True,
                                           validHostIds=[host1.id],
                                           ports=[
                                               '8082:81/tcp'])
        wait_for_condition(
            super_client, c3,
            lambda x: x.state == 'running')
        containers.append(c3)

        # try different protocol
        c4 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           startOnCreate=True,
                                           validHostIds=[host1.id],
                                           ports=[
                                               '8081:81/udp'])
        wait_for_condition(
            super_client, c4,
            lambda x: x.state == 'running')
        containers.append(c4)

        c5 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           validHostIds=[host1.id],
                                           startOnCreate=True,
                                           ports=['8081:81/udp'])
        c5 = wait_transitioning(super_client, c5)
        assert c5.transitioning == 'error'
        assert c5.transitioningMessage == 'Failed to find a placement'
        assert c5.state == 'removed'
    finally:
        for c in containers:
            if c is not None:
                super_client.wait_success(super_client.delete(c))


def test_request_host_override(super_client, sim_context, network):
    image_uuid = sim_context['imageUuid']
    host = sim_context['host']

    c = None
    c2 = None

    try:
        c = super_client.create_container(imageUuid=image_uuid,
                                          networkIds=[network.id],
                                          startOnCreate=True,
                                          validHostIds=[host.id],
                                          ports=['8081:81/tcp'])
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')

        # try to deploy another container with same public port + protocol
        # however, explicitly specify requestedHostId
        c2 = super_client.create_container(imageUuid=image_uuid,
                                           networkIds=[network.id],
                                           validHostIds=[host.id],
                                           requestedHostId=host.id,
                                           startOnCreate=True,
                                           ports=['8081:81/tcp'])
        wait_for_condition(
            super_client, c2,
            lambda x: x.state == 'running')

    finally:
        if c is not None:
            super_client.wait_success(super_client.delete(c))
        if c2 is not None:
            super_client.wait_success(super_client.delete(c2))


def test_host_affinity(super_client, sim_context, sim_context2, network):
    image_uuid = sim_context['imageUuid']
    host = sim_context['host']
    host2 = sim_context2['host']

    validHostIds = [host.id, host2.id]

    host.addlabel(key='size', value='huge')
    host.addlabel(key='latency', value='long')
    host2.addlabel(key='size', value='tiny')
    host2.addlabel(key='latency', value='short')

    containers = []
    try:
        # test affinity
        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'constraint:size==huge': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host.id
        containers.append(c)

        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'io.rancher.scheduler.constraint:size{eq}huge': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host.id
        containers.append(c)

        # test anti-affinity
        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'constraint:size!=huge': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'io.rancher.scheduler.constraint:size{ne}huge': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        # test soft affinity.
        # prefer size==huge, but latency==~short if possible
        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={
                'constraint:size==huge': '',
                'constraint:latency==~short': ''
            })
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host.id
        containers.append(c)

        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={
                'io.rancher.scheduler.constraint:size{eq}huge': '',
                'io.rancher.scheduler.constraint:latency{ne~}short': ''
            })
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host.id
        containers.append(c)

        # test soft anti-affinity
        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'constraint:latency!=~long': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        c = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'io.rancher.scheduler.constraint:latency{ne~}long': ''})
        wait_for_condition(
            super_client, c,
            lambda x: x.state == 'running')
        assert c.hosts()[0].id == host2.id
        containers.append(c)

    finally:
        for c in containers:
            if c is not None:
                super_client.wait_success(super_client.delete(c))


def test_container_affinity(super_client, sim_context, sim_context2, network):
    image_uuid = sim_context['imageUuid']
    host = sim_context['host']
    host2 = sim_context2['host']

    validHostIds = [host.id, host2.id]
    containers = []
    try:
        name1 = 'affinity' + random_str()
        c1 = super_client.create_container(
            imageUuid=image_uuid,
            name=name1,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds)
        wait_for_condition(
            super_client, c1,
            lambda x: x.state == 'running')
        containers.append(c1)

        c2 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'affinity:container==' + name1: ''})
        wait_for_condition(
            super_client, c2,
            lambda x: x.state == 'running')
        containers.append(c2)

        # check c2 is on same host as c1
        assert c2.hosts()[0].id == c1.hosts()[0].id

        c3 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'io.rancher.scheduler.affinity:container{eq}' + name1: ''})
        wait_for_condition(
            super_client, c3,
            lambda x: x.state == 'running')
        containers.append(c3)

        # check c3 is on same host as c1
        assert c3.hosts()[0].id == c1.hosts()[0].id

        c4 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'affinity:container==' + c1.uuid: ''})
        wait_for_condition(
            super_client, c4,
            lambda x: x.state == 'running')
        containers.append(c4)

        # check c4 is on same host as c1
        assert c4.hosts()[0].id == c1.hosts()[0].id

        c5 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={
                'io.rancher.scheduler.affinity:container{eq}' + c1.uuid: ''})
        wait_for_condition(
            super_client, c5,
            lambda x: x.state == 'running')
        containers.append(c5)

        # check c5 is on same host as c1
        assert c5.hosts()[0].id == c1.hosts()[0].id

        c6 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'affinity:container!=' + name1: ''})
        wait_for_condition(
            super_client, c6,
            lambda x: x.state == 'running')
        containers.append(c6)

        # check c6 is not on same host as c1
        assert c6.hosts()[0].id != c1.hosts()[0].id

        c7 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'io.rancher.scheduler.affinity:container{ne}' + name1: ''})
        wait_for_condition(
            super_client, c7,
            lambda x: x.state == 'running')
        containers.append(c7)

        # check c7 is not on same host as c1
        assert c7.hosts()[0].id != c1.hosts()[0].id

    finally:
        for c in containers:
            if c is not None:
                super_client.wait_success(super_client.delete(c))


def test_container_label_affinity(
        super_client, sim_context, sim_context2, network):
    image_uuid = sim_context['imageUuid']
    host = sim_context['host']
    host2 = sim_context2['host']

    validHostIds = [host.id, host2.id]
    containers = []
    try:
        c1_label = random_str()
        c1 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={'foo': c1_label}
        )
        wait_for_condition(
            super_client, c1,
            lambda x: x.state == 'running')
        containers.append(c1)

        c2 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'affinity:container_label:foo==' + c1_label: ''})
        wait_for_condition(
            super_client, c2,
            lambda x: x.state == 'running')
        containers.append(c2)

        # check c2 is on same host as c1
        assert c2.hosts()[0].id == c1.hosts()[0].id

        c3 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            labels={
                'io.rancher.scheduler.affinity:container_label:foo{eq}'
                + c1_label: ''}
        )
        wait_for_condition(
            super_client, c3,
            lambda x: x.state == 'running')
        containers.append(c3)

        # check c3 is on same host as c1
        assert c3.hosts()[0].id == c1.hosts()[0].id

        c4_label = random_str()

        c4 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={'affinity:container_label:foo!=' + c1_label: ''},
            labels={'foo': c4_label}
        )
        wait_for_condition(
            super_client, c4,
            lambda x: x.state == 'running')
        containers.append(c4)

        # check c4 is not on same host as c1
        assert c4.hosts()[0].id != c1.hosts()[0].id

        c5 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={
                'affinity:container_label:foo!=' + c1_label: '',
                'affinity:container_label:foo!=~' + c4_label: ''
            })
        wait_for_condition(
            super_client, c5,
            lambda x: x.state == 'running')
        containers.append(c5)

        # since we just specified a soft anti-affinity to c4,
        # check c5 is on same host as c4
        assert c5.hosts()[0].id == c4.hosts()[0].id

        c6 = super_client.create_container(
            imageUuid=image_uuid,
            networkIds=[network.id],
            startOnCreate=True,
            validHostIds=validHostIds,
            environment={
                'affinity:container_label:foo!=' + c1_label: '',
            },
            labels={
                'io.rancher.scheduler.affinity:container_label:foo{ne~}'
                + c4_label: ''
            }
        )
        wait_for_condition(
            super_client, c6,
            lambda x: x.state == 'running')
        containers.append(c6)

        assert c6.hosts()[0].id == c4.hosts()[0].id

    finally:
        for c in containers:
            if c is not None:
                super_client.wait_success(super_client.delete(c))
