from common_fixtures import *  # NOQA


def test_container_port_create_start(super_client, new_context):
    image_uuid = new_context.image_uuid
    host = new_context.host
    host_ip = new_context.host_ip.address
    client = new_context.client
    assert host_ip is not None

    c = client.create_container(imageUuid=image_uuid,
                                startOnCreate=False,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])
    assert c.state == 'creating'
    c = client.wait_success(c)

    assert c.state == 'stopped'

    c_admin = super_client.update(c, requestedHostId=host.id)
    assert c_admin.requestedHostId == host.id

    ports = c.ports_link()
    assert len(ports) == 3

    count = 0
    for port in ports:
        assert port.kind == 'userPort'
        if port.privatePort == 80:
            count += 1
            assert port.publicPort is None
            assert port.protocol == 'tcp'
            assert port.instanceId == c.id
            assert port.state == 'inactive'
        elif port.privatePort == 81:
            count += 1
            assert port.publicPort == 8081
            assert port.protocol == 'tcp'
            assert port.instanceId == c.id
            assert port.state == 'inactive'
        elif port.privatePort == 82:
            count += 1
            assert port.publicPort == 8082
            assert port.protocol == 'udp'
            assert port.instanceId == c.id
            assert port.state == 'inactive'

    assert count == 3

    c = client.wait_success(c.start())
    assert super_client.reload(c).hosts()[0].id == host.id

    for port in c.ports_link():
        assert port.state == 'active'
        private_ip = port.privateIpAddress()
        public_ip = port.publicIpAddress()
        assert private_ip.address == c.primaryIpAddress
        assert public_ip.address == host_ip
        assert port.id in [x.id for x in private_ip.privatePorts()]
        assert port.id in [x.id for x in public_ip.publicPorts()]
        assert port.id not in [x.id for x in public_ip.privatePorts()]
        assert port.id not in [x.id for x in private_ip.publicPorts()]


def test_container_port_start(new_context):
    client = new_context.client
    image_uuid = new_context.image_uuid
    c = client.create_container(imageUuid=image_uuid,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

    assert c.state == 'creating'
    c = client.wait_success(c)

    assert c.state == 'running'

    ports = c.ports_link()
    assert len(ports) == 3

    count = 0
    for port in ports:
        if port.privatePort == 80:
            count += 1
            assert port.protocol == 'tcp'
            assert port.instanceId == c.id
            assert port.state == 'active'
        elif port.privatePort == 81:
            count += 1
            assert port.publicPort == 8081
            assert port.protocol == 'tcp'
            assert port.instanceId == c.id
            assert port.state == 'active'
        elif port.privatePort == 82:
            count += 1
            assert port.publicPort == 8082
            assert port.protocol == 'udp'
            assert port.instanceId == c.id
            assert port.state == 'active'

    assert count == 3


def test_container_port_stop(new_context):
    client = new_context.client
    image_uuid = new_context.image_uuid
    c = client.create_container(imageUuid=image_uuid,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

    assert c.state == 'creating'
    c = client.wait_success(c)
    assert c.state == 'running'

    c = client.wait_success(c.stop())
    assert c.state == 'stopped'

    ports = c.ports_link()
    assert len(ports) == 3

    count = 0
    for port in ports:
        assert port.state == 'inactive'
        assert port.publicIpAddressId is not None
        assert port.privateIpAddressId is not None
        if port.privatePort == 80:
            count += 1
        elif port.privatePort == 81:
            count += 1
            assert port.publicPort == 8081
        elif port.privatePort == 82:
            count += 1
            assert port.publicPort == 8082

    assert count == 3


def test_container_port_purge(new_context):
    client = new_context.client
    image_uuid = new_context.image_uuid
    c = client.create_container(imageUuid=image_uuid,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

    assert c.state == 'creating'
    c = client.wait_success(c)
    assert c.state == 'running'

    c = client.wait_success(c.stop(remove=True))
    assert c.state == 'removed'

    assert len(c.ports_link()) == 3

    c = client.wait_success(c.purge())
    assert c.state == 'purged'
    assert len(c.ports_link()) == 0


def test_port_validation(client, context):
    try:
        client.create_container(imageUuid=context.image_uuid,
                                ports=['a'])
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'PortWrongFormat'


def test_ports_service(super_client, new_context):
    client = new_context.client
    c = new_context.create_container(ports=['80'])

    agent = super_client.reload(c).hosts()[0].agent()
    assert agent is not None

    port = c.ports_link()[0]

    assert port.publicPort is None

    port = client.update(port, publicPort=12345)
    assert port.state == 'updating-active'
    assert port.publicPort == 12345

    port = client.wait_success(port)
    assert port.state == 'active'


def test_ports_overlapping(new_context):
    port_specs = [
        '1234:80/tcp',
        '2345:80/tcp',
        '1234:80/udp',
        '2345:80/udp',
    ]
    c = new_context.create_container(ports=port_specs)

    ports = c.ports_link()

    assert len(ports) == 4

    found = {'{}:{}/{}'.format(x.publicPort, x.privatePort, x.protocol)
             for x in ports}

    assert set(port_specs) == found
