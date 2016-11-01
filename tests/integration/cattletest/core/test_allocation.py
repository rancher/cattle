from common_fixtures import *  # NOQA
from test_shared_volumes import add_storage_pool


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
        'Scheduling failed: No healthy hosts with sufficient ' \
        'resources available'
    assert c.state == 'error'


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
    assert v1.state == 'inactive'

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


def test_allocation_stay_associated_to_host(super_client, context):
    c = context.create_container()
    c = context.client.wait_success(c.stop())
    assert c.state == 'stopped'

    assert len(c.hosts()) == 1


def test_port_constraint(new_context):
    host1 = new_context.host
    client = new_context.client
    image_uuid = new_context.image_uuid

    containers = []

    try:
        c = client.wait_success(
            client.create_container(imageUuid=image_uuid,
                                    requestedHostId=host1.id,
                                    ports=['8081:81/tcp']))
        containers.append(c)

        # try to deploy another container with same public port + protocol
        c2 = client.wait_transitioning(
            client.create_container(imageUuid=image_uuid,
                                    ports=['8081:81/tcp']))
        assert c2.transitioning == 'error'
        assert c2.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/tcp available'
        assert c2.state == 'error'

        # try different public port
        c3 = new_context.super_create_container(imageUuid=image_uuid,
                                                ports=['8082:81/tcp'])
        containers.append(c3)

        # try different protocol
        c4 = client.wait_success(
            client.create_container(imageUuid=image_uuid,
                                    ports=['8081:81/udp']))
        containers.append(c4)

        # UDP is now taken
        c5 = client.wait_transitioning(
            client.create_container(imageUuid=image_uuid,
                                    ports=['8081:81/udp']))
        assert c5.transitioning == 'error'
        assert c5.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/udp available'
        assert c5.state == 'error'

        # try different bind IP
        c6 = client.wait_success(
            client.create_container(imageUuid=image_uuid,
                                    requestedHostId=host1.id,
                                    ports=['127.2.2.2:8081:81/tcp']))
        containers.append(c6)

        # Bind IP is now taken
        c7 = client.wait_transitioning(
            client.create_container(imageUuid=image_uuid,
                                    ports=['127.2.2.2:8081:81/tcp']))
        assert c7.transitioning == 'error'
        assert c7.transitioningMessage == \
            'Scheduling failed: host needs ports 8081/tcp available'
        assert c7.state == 'error'

        # increase host pool and check whether allocator picks other host
        host2 = register_simulated_host(new_context.client)
        c8 = client.wait_success(
            client.create_container(imageUuid=image_uuid,
                                    ports=['8081:81/tcp']))
        assert c8.hosts()[0].id == host2.id
        containers.append(c8)
    finally:
        for c in containers:
            if c is not None:
                new_context.delete(c)


def test_conflicting_ports_in_deployment_unit(new_context):
    client = new_context.client
    image_uuid = new_context.image_uuid
    client.wait_success(client.create_container(name='reset',
                                                imageUuid=image_uuid))

    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid, "ports": ['5555:6666']}
    secondary_lc = {"imageUuid": image_uuid,
                    "name": "secondary", "ports": ['5555:6666']}

    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                secondaryLaunchConfigs=[secondary_lc])
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc = svc.activate()
    c = _wait_for_compose_instance_error(client, svc, env)
    assert 'Port 5555/tcp requested more than once.' in c.transitioningMessage
    env.remove()


def test_simultaneous_port_allocation(new_context):
    # This test ensures if two containers are allocated simultaneously, only
    # one will get the port and the other will fail to allocate.
    # By nature, this test is exercise a race condition, so it isn't perfect.
    client = new_context.client
    image_uuid = new_context.image_uuid

    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == "active"

    launch_config = {"imageUuid": image_uuid, "ports": ['5555:6666']}
    svc = client.create_service(name=random_str(),
                                stackId=env.id,
                                launchConfig=launch_config,
                                scale=2)
    svc = client.wait_success(svc)
    assert svc.state == "inactive"

    svc = svc.activate()
    c = _wait_for_compose_instance_error(client, svc, env)
    assert 'host needs ports 5555/tcp available' in c.transitioningMessage


def _wait_for_compose_instance_error(client, service, env):
    name = env.name + "-" + service.name + "%"
    wait_for(
        lambda: len(client.list_container(name_like=name, state='error')) > 0
    )
    return client.list_container(name_like=name, state='error')[0]


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
        c4 = new_context.client.wait_transitioning(c4)
        assert c4.transitioning == 'error'
        assert c4.transitioningMessage == 'volumeFrom instance is not ' \
                                          'running : Dependencies readiness' \
                                          ' error'
    finally:
        for c in containers:
            new_context.delete(c)


def test_network_mode_constraint(new_context):
    client = new_context.client

    # Three hosts
    register_simulated_host(new_context)
    register_simulated_host(new_context)

    containers = []
    try:
        c1 = new_context.create_container(startOnCreate=False)

        c2 = new_context.create_container(startOnCreate=False,
                                          networkMode='container',
                                          networkContainerId=c1.id)

        c1 = client.wait_success(c1.start())
        c2 = client.wait_success(c2.start())

        assert c1.state == 'running'
        containers.append(c1)

        assert c1.state == 'running'
        containers.append(c2)

        assert c1.hosts()[0].id == c2.hosts()[0].id

    finally:
        for c in containers:
            new_context.delete(c)
