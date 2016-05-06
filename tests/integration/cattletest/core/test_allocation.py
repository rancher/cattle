from common_fixtures import *  # NOQA
from test_shared_volumes import add_storage_pool


def test_compute_free(super_client, new_context):
    admin_client = new_context.client
    count = 5
    host = super_client.reload(new_context.host)
    image_uuid = new_context.image_uuid
    start_free = host.computeFree

    assert start_free > count

    containers = []
    for _ in range(count):
        c = admin_client.create_container(imageUuid=image_uuid,
                                          networkMode='bridge',
                                          requestedHostId=host.id)
        containers.append(c)

    containers = wait_all_success(super_client, containers)

    host = super_client.reload(host)
    assert host.computeFree == start_free - count

    for c in containers:
        super_client.delete(c)
        c = super_client.wait_success(c)
        c = super_client.wait_success(c.purge())

    wait_all_success(admin_client, containers)
    for c in containers:
        c = super_client.reload(c)
        wait_for(lambda: super_client.reload(c).allocationState == 'inactive')

    host = super_client.reload(host)
    assert host.computeFree == start_free


def test_inactive_agent(super_client, new_context):
    host = super_client.reload(new_context.host)
    agent = host.agent()

    c = new_context.create_container()
    assert c.state == 'running'

    agent = super_client.wait_success(agent.deactivate())
    assert agent.state == 'inactive'

    c = new_context.create_container_no_success()
    assert c.transitioning == 'error'
    assert c.transitioningMessage == \
        'Scheduling failed: No candidates available'
    assert c.state == 'error'


def test_spread(super_client, new_context):
    count = 3

    client = new_context.client
    host2 = register_simulated_host(new_context.client)
    host3 = register_simulated_host(new_context.client)

    hosts = [new_context.host, host2, host3]
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
        c = client.create_container(imageUuid=new_context.image_uuid,
                                    networkMode='bridge')
        containers.append(c)

    wait_all_success(super_client, containers, timeout=60)

    for i, h in enumerate(hosts):
        h = super_client.reload(h)
        assert counts[i] - count == h.computeFree


def test_allocation_with_shared_storage_pool(super_client, new_context):
    count = 3

    client = new_context.client
    host2 = register_simulated_host(client)
    register_simulated_host(client)

    hosts = [new_context.host, host2]
    hosts = wait_all_success(super_client, hosts)
    sp = add_storage_pool(new_context, [new_context.host.uuid, host2.uuid])
    sp_name = sp.name
    for h in hosts:
        assert h.state == 'active'
        assert h.agent().state == 'active'
        assert len(h.storagePools()) == 2
        assert h.storagePools()[0].state == 'active'
        assert h.storagePools()[1].state == 'active'

    # Create a volume with a driver that points to a storage pool
    v1 = client.create_volume(name=random_str(), driver=sp_name)
    v1 = client.wait_success(v1)
    assert v1.state == 'requested'

    data_volume_mounts = {'/con/path': v1.id}

    containers = []
    for _ in range(len(hosts) * count):
        c = client.create_container(imageUuid=new_context.image_uuid,
                                    dataVolumeMounts=data_volume_mounts)
        containers.append(c)
        time.sleep(1)  # Sleep makes the test faster as it reduces contention

    wait_all_success(super_client, containers, timeout=60)
    for c in containers:
        new_context.wait_for_state(c, 'running')


def test_allocate_to_host_with_pool(new_context, super_client):
    # If a volumeDriver is specified that maps to an existing pool, restrict
    # allocation to hosts in that pool
    client = new_context.client
    host = new_context.host
    host2 = register_simulated_host(client)

    sp = add_storage_pool(new_context)
    sp_name = sp.name
    assert len(host.storagePools()) == 2
    assert len(host2.storagePools()) == 1

    # Fail to schedule because requested host is not in pool
    c = new_context.create_container_no_success(
        imageUuid=new_context.image_uuid,
        volumeDriver=sp_name,
        requestedHostId=host2.id,
        dataVolume=['vol1:/con/path'])
    c = super_client.reload(c)
    assert c.state == 'error'
    assert c.transitioning == 'error'
    assert c.transitioningMessage.startswith(
        'Scheduling failed: valid host(s) [')


def test_host_vnet_association(super_client, new_context):
    account = new_context.project
    image_uuid = new_context.image_uuid
    host1 = new_context.host
    host2 = register_simulated_host(new_context.client)
    host3 = register_simulated_host(new_context.client)

    host1 = super_client.update(host1, computeFree=100000)
    host2 = super_client.update(host2, computeFree=100000)
    host3 = super_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    network = super_client.create_network(accountId=account.id)
    vnet = super_client.create_vnet(accountId=account.id,
                                    networkId=network.id,
                                    uri='sim://')
    vnet = super_client.wait_success(vnet)

    assert vnet.state == 'active'

    subnet1 = super_client.create_subnet(accountId=account.id,
                                         networkAddress='192.168.0.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.0.3',
                                         endAddress='192.168.0.5')
    subnet1 = super_client.wait_success(subnet1)

    subnet2 = super_client.create_subnet(accountId=account.id,
                                         networkAddress='192.168.2.0',
                                         cidrSize='16',
                                         networkId=network.id,
                                         startAddress='192.168.2.3',
                                         endAddress='192.168.3.5')
    subnet2 = super_client.wait_success(subnet2)

    subnet_map1 = super_client.create_subnet_vnet_map(accountId=account.id,
                                                      subnetId=subnet1.id,
                                                      vnetId=vnet.id)
    subnet_map1 = super_client.wait_success(subnet_map1)
    assert subnet_map1.state == 'active'

    subnet_map2 = super_client.create_subnet_vnet_map(accountId=account.id,
                                                      subnetId=subnet2.id,
                                                      vnetId=vnet.id)
    subnet_map2 = super_client.wait_success(subnet_map2)
    assert subnet_map2.state == 'active'

    vnet_map1 = super_client.create_host_vnet_map(accountId=account.id,
                                                  hostId=host1.id,
                                                  vnetId=vnet.id)
    vnet_map1 = super_client.wait_success(vnet_map1)
    assert vnet_map1.state == 'active'

    vnet_map2 = super_client.create_host_vnet_map(accountId=account.id,
                                                  hostId=host2.id,
                                                  vnetId=vnet.id)
    vnet_map2 = super_client.wait_success(vnet_map2)
    assert vnet_map2.state == 'active'

    hosts = set()
    for _ in range(3):
        vm = super_client.create_virtual_machine(accountId=account.id,
                                                 subnetIds=[subnet1.id],
                                                 imageUuid=image_uuid)
        vm = super_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    for _ in range(3):
        vm = super_client.create_virtual_machine(accountId=account.id,
                                                 subnetIds=[subnet2.id],
                                                 imageUuid=image_uuid)
        vm = super_client.wait_success(vm)
        assert vm.state == 'running'
        hosts.add(vm.hosts()[0].id)

    assert len(hosts) == 2
    assert host1.id in hosts
    assert host2.id in hosts


def test_allocation_stay_associated_to_host(super_client, context):
    c = context.create_container()
    c = context.client.wait_success(c.stop())
    assert c.state == 'stopped'

    assert len(c.hosts()) == 1


def test_vnet_stickiness(super_client, new_context):
    account_id = new_context.project.id
    network = super_client.list_network(accountId=account_id,
                                        kind='hostOnlyNetwork')[0]
    subnet = super_client.list_subnet(accountId=account_id)[0]

    image_uuid = new_context.image_uuid
    host1 = new_context.host
    host2 = register_simulated_host(new_context.client)
    host3 = register_simulated_host(new_context.client)
    valid_hosts = [host1.id, host2.id, host3.id]

    host1 = super_client.update(host1, computeFree=100000)
    host2 = super_client.update(host2, computeFree=100000)
    host3 = super_client.update(host3, computeFree=100000)
    for i in [host1, host2, host3]:
        assert i.computeFree == 100000

    containers = []
    for _ in range(3):
        c = super_client.reload(new_context.create_container())
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
        c = super_client.create_container(accountId=account_id,
                                          imageUuid=image_uuid,
                                          vnetIds=[c1_nic.vnetId])
        c = super_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId

    for _ in range(3):
        c = super_client.create_container(accountId=account_id,
                                          imageUuid=image_uuid,
                                          networkIds=[network.id],
                                          vnetIds=[c1_nic.vnetId])
        c = super_client.wait_success(c)

        assert c.hosts()[0].id == c1_host_id
        nic = c.nics()[0]

        assert nic.subnetId == c1_nic.subnetId
        assert nic.vnetId == c1_nic.vnetId
        assert nic.networkId == c1_nic.networkId


def test_port_constraint(new_context):
    host1 = new_context.host
    host2 = register_simulated_host(new_context.client)

    containers = []

    try:
        c = new_context.create_container(requestedHostId=host1.id,
                                         ports=['8081:81/tcp'])
        containers.append(c)

        # try to deploy another container with same public port + protocol
        c2 = new_context\
            .super_create_container_no_success(validHostIds=[host1.id],
                                               ports=['8081:81/tcp'])
        assert c2.transitioning == 'error'
        assert c2.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/tcp available'
        assert c2.state == 'error'

        # increase host pool and check whether allocator picks other host
        c2 = new_context.super_create_container(validHostIds=[host1.id,
                                                              host2.id],
                                                ports=['8081:81/tcp'])
        containers.append(c2)

        # try different public port
        c3 = new_context.super_create_container(validHostIds=[host1.id],
                                                ports=['8082:81/tcp'])
        containers.append(c3)

        # try different protocol
        c4 = new_context.super_create_container(validHostIds=[host1.id],
                                                ports=['8081:81/udp'])
        containers.append(c4)

        c5 = new_context\
            .super_create_container_no_success(validHostIds=[host1.id],
                                               ports=['8081:81/udp'])
        assert c5.transitioning == 'error'
        assert c5.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/udp available'
        assert c5.state == 'error'

        # try different bind IP
        c6 = new_context.\
            super_create_container(validHostIds=[host1.id],
                                   ports=['127.2.2.2:8081:81/tcp'])
        containers.append(c6)

        c7 = new_context \
            .super_create_container_no_success(validHostIds=[host1.id],
                                               ports=['127.2.2.2:8081:81/tcp'])
        assert c7.transitioning == 'error'
        assert c7.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/tcp available'
        assert c7.state == 'error'
    finally:
        for c in containers:
            if c is not None:
                new_context.delete(c)


def test_request_host_override(new_context):
    host = new_context.host
    c = None
    c2 = None

    try:
        c = new_context.super_create_container(validHostIds=[host.id],
                                               ports=['8081:81/tcp'])

        # try to deploy another container with same public port + protocol
        # however, explicitly specify requestedHostId
        c2 = new_context.super_create_container(requestedHostId=host.id,
                                                ports=['8081:81/tcp'])
    finally:
        if c is not None:
            new_context.delete(c)
        if c2 is not None:
            new_context.delete(c2)


def test_host_affinity(super_client, new_context):
    host = new_context.host
    host2 = register_simulated_host(new_context)

    host = super_client.update(host, labels={'size': 'huge',
                                             'latency': 'long'})

    host2 = super_client.update(host2, labels={'size': 'tiny',
                                               'latency': 'short'})

    containers = []
    try:
        # test affinity
        c = new_context.create_container(
            environment={'constraint:size==huge': ''})
        assert c.hosts()[0].id == host.id
        containers.append(c)

        c = new_context.create_container(
            labels={'io.rancher.scheduler.affinity:host_label': 'size=huge'})
        assert c.hosts()[0].id == host.id
        containers.append(c)

        # test anti-affinity
        c = new_context.create_container(
            environment={'constraint:size!=huge': ''})
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        c = new_context.create_container(
            labels={'io.rancher.scheduler.affinity:host_label_ne':
                    'size=huge'})
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        # test soft affinity.
        # prefer size==huge, but latency==~short if possible
        c = new_context.create_container(
            environment={
                'constraint:size==huge': '',
                'constraint:latency==~short': ''
            })
        assert c.hosts()[0].id == host.id
        containers.append(c)

        c = new_context.create_container(
            labels={
                'io.rancher.scheduler.affinity:host_label': 'size=huge',
                'io.rancher.scheduler.affinity:host_label_soft_ne':
                    'latency=short'
            })
        assert c.hosts()[0].id == host.id
        containers.append(c)

        # test soft anti-affinity
        c = new_context.create_container(
            environment={'constraint:latency!=~long': ''})
        assert c.hosts()[0].id == host2.id
        containers.append(c)

        c = new_context.create_container(
            labels={'io.rancher.scheduler.affinity:host_label_soft_ne':
                    'latency=long'})
        assert c.hosts()[0].id == host2.id
        containers.append(c)
    finally:
        for c in containers:
            new_context.delete(c)


def test_container_affinity(new_context):
    # Two hosts
    register_simulated_host(new_context)

    containers = []
    try:
        name1 = 'affinity' + random_str()
        c1 = new_context.create_container(
            name=name1)
        containers.append(c1)

        c2 = new_context.create_container(
            environment={'affinity:container==' + name1: ''})
        containers.append(c2)

        # check c2 is on same host as c1
        assert c2.hosts()[0].id == c1.hosts()[0].id

        c3 = new_context.create_container(
            labels={'io.rancher.scheduler.affinity:container': name1})
        containers.append(c3)

        # check c3 is on same host as c1
        assert c3.hosts()[0].id == c1.hosts()[0].id

        c4 = new_context.create_container(
            environment={'affinity:container==' + c1.uuid: ''})
        containers.append(c4)

        # check c4 is on same host as c1
        assert c4.hosts()[0].id == c1.hosts()[0].id

        c5 = new_context.create_container(
            labels={
                'io.rancher.scheduler.affinity:container': c1.uuid})
        containers.append(c5)

        # check c5 is on same host as c1
        assert c5.hosts()[0].id == c1.hosts()[0].id

        c6 = new_context.create_container(
            environment={'affinity:container!=' + name1: ''})
        containers.append(c6)

        # check c6 is not on same host as c1
        assert c6.hosts()[0].id != c1.hosts()[0].id

        c7 = new_context.create_container(
            labels={'io.rancher.scheduler.affinity:container_ne': name1})
        containers.append(c7)

        # check c7 is not on same host as c1
        assert c7.hosts()[0].id != c1.hosts()[0].id
    finally:
        for c in containers:
            new_context.delete(c)


def test_container_label_affinity(new_context):
    # Two hosts
    register_simulated_host(new_context)

    containers = []
    try:
        c1_label = random_str()
        c1 = new_context.create_container(
            labels={'foo': c1_label}
        )
        containers.append(c1)

        c2 = new_context.create_container(
            environment={'affinity:foo==' + c1_label: ''})
        containers.append(c2)

        # check c2 is on same host as c1
        assert c2.hosts()[0].id == c1.hosts()[0].id

        c3 = new_context.create_container(
            labels={
                'io.rancher.scheduler.affinity:container_label':
                    'foo=' + c1_label}
        )
        containers.append(c3)

        # check c3 is on same host as c1
        assert c3.hosts()[0].id == c1.hosts()[0].id

        c4_label = random_str()

        c4 = new_context.create_container(
            environment={'affinity:foo!=' + c1_label: ''},
            labels={'foo': c4_label}
        )
        containers.append(c4)

        # check c4 is not on same host as c1
        assert c4.hosts()[0].id != c1.hosts()[0].id

        c5 = new_context.create_container(
            environment={
                'affinity:foo!=' + c1_label: '',
                'affinity:foo!=~' + c4_label: ''
            })
        containers.append(c5)

        # since we just specified a soft anti-affinity to c4,
        # check c5 is on same host as c4
        assert c5.hosts()[0].id == c4.hosts()[0].id

        c6 = new_context.create_container(
            environment={
                'affinity:foo!=' + c1_label: '',
            },
            labels={
                'io.rancher.scheduler.affinity:container_label_soft_ne':
                'foo=' + c4_label
            }
        )
        containers.append(c6)

        assert c6.hosts()[0].id == c4.hosts()[0].id
    finally:
        for c in containers:
            new_context.delete(c)


def test_volumes_from_constraint(new_context):
    # Three hosts
    register_simulated_host(new_context)
    register_simulated_host(new_context)

    containers = []
    try:
        # nominal condition.  start c1 before c2
        c1 = new_context.create_container_no_success(startOnCreate=False)

        c2 = new_context.create_container_no_success(startOnCreate=False,
                                                     dataVolumesFrom=[c1.id])

        c1 = c1.start()
        c2 = c2.start()
        c1 = new_context.wait_for_state(c1, 'running')
        c2 = new_context.wait_for_state(c2, 'running')

        containers.append(c1)
        containers.append(c2)

        assert c1.hosts()[0].id == c2.hosts()[0].id

        # less than ideal situation.  start c4 before c3
        c3 = new_context.create_container_no_success(startOnCreate=False)
        c4 = new_context.create_container_no_success(startOnCreate=False,
                                                     dataVolumesFrom=[c3.id])

        c4 = c4.start()
        c3 = c3.start()
        c4 = new_context.wait_for_state(c4, 'running')
        c3 = new_context.wait_for_state(c3, 'running')
        containers.append(c3)
        containers.append(c4)

        assert c3.hosts()[0].id == c4.hosts()[0].id
    finally:
        for c in containers:
            new_context.delete(c)


def test_network_mode_constraint(new_context):
    # Three hosts
    register_simulated_host(new_context)
    register_simulated_host(new_context)

    containers = []
    try:
        c1 = new_context.create_container_no_success(startOnCreate=False)

        c2 = new_context.create_container(startOnCreate=False,
                                          networkMode='container',
                                          networkContainerId=c1.id)

        c1 = c1.start()
        c2 = c2.start()

        c1 = new_context.wait_for_state(c1, 'running')
        containers.append(c1)

        c2 = new_context.wait_for_state(c2, 'running')
        containers.append(c2)

        assert c1.hosts()[0].id == c2.hosts()[0].id

    finally:
        for c in containers:
            new_context.delete(c)


def test_container_label_disksize_single_volume(super_client, new_context):
    # first host with really small disk size
    host1 = new_context.host
    containers = []
    try:
        # set host1 with disk size too small, verify that scheduling a container will fail
        diskInfo1 = {"diskInfo": {"mountPoints": {"/dev/sda1": {"total": 10.0, "used": 1.0}}}}
        host1 = super_client.update(host1, info=diskInfo1)

        # schedule a container requiring 50 disk size
        label = 'io.rancher.scheduler.disksize.v1'
        c1 = new_context.super_create_container_no_success(labels={label: '50'})
        containers.append(c1)
        assert c1.transitioning == 'error'
        assert c1.transitioningMessage == \
            'Scheduling failed: host needs a disk with free space larger than 50 GB'
        assert c1.state == 'error'


        # add a host2 with enough disk size
        host2 = register_simulated_host(new_context)
        diskInfo2 = {"diskInfo": {"mountPoints": {"/dev/sda1": {"total": 100.0, "used" : 1.0}}}}
        host2 = super_client.update(host2, info=diskInfo2)
        c2 = new_context.create_container(labels={label: '50'})
        containers.append(c2)

        # check c2 is on host2 with enough disk space
        assert c2.hosts()[0].id == host2.id

        # now schedule another 50GB container on the host2, expect it to fail
        # because host2 already used 1 + 50 GB disk space with total only 100
        # so can't schedule more now
        c3 = new_context.super_create_container_no_success(labels={label: '50'})
        containers.append(c3)
        assert c3.transitioning == 'error'
        assert c3.transitioningMessage == \
               'Scheduling failed: host needs a disk with free space larger than 50 GB'
        assert c3.state == 'error'

    finally:
        for c in containers:
            new_context.delete(c)


def test_container_label_disksize_multiple_volumes(super_client, new_context):
    # first host with really small disk size
    host1 = new_context.host
    containers = []
    try:
        # set host1 with disks size too small, verify that scheduling a container will fail
        diskInfo1 = {"diskInfo": {"mountPoints": {"/dev/sda1": {"total": 10.0, "used": 1.0},
                                                  "/dev/sdb1": {"total": 10.0, "used": 0.0}}}}
        host1 = super_client.update(host1, info=diskInfo1)

        # schedule a container requiring 5 and 50 disk size volumes
        label1 = 'io.rancher.scheduler.disksize.v1'
        label2 = 'io.rancher.scheduler.disksize.v2'
        c1 = new_context.super_create_container_no_success(
            labels = {
                label1 : '5',
                label2 : '50'
            }
        )
        containers.append(c1)
        assert c1.transitioning == 'error'
        assert c1.transitioningMessage == \
            'Scheduling failed: host needs a disk with free space larger than 50 GB'
        assert c1.state == 'error'


        # add a host2 with enough disks size with 2 disks
        host2 = register_simulated_host(new_context)
        diskInfo2 = {"diskInfo": {"mountPoints": {"/dev/sda1": {"total": 10.0, "used": 1.0},
                                                  "/dev/sdb1": {"total": 100.0, "used": 0.0}}}}
        host2 = super_client.update(host2, info=diskInfo2)
        c2 = new_context.super_create_container_no_success(
            labels = {
                label1 : '5',
                label2 : '50'
            }
        )
        containers.append(c2)

        # check c2 is on host2 with enough disk space
        assert c2.hosts()[0].id == host2.id

    finally:
        for c in containers:
            new_context.delete(c)
