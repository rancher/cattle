from common_fixtures import *  # NOQA


def test_container_port_create_start(client, super_client, context):
    image_uuid = context.image_uuid
    host = context.host
    host_ip = context.host_ip.address
    assert host_ip is not None

    c = client.create_container(imageUuid=image_uuid,
                                startOnCreate=False,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])
    try:
        assert c.state == 'creating'
        c = client.wait_success(c)

        assert c.state == 'stopped'

        c_admin = super_client.update(c, requestedHostId=host.id)
        assert c_admin.requestedHostId == host.id

        ports = c.ports()
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

        for port in c.ports():
            assert port.state == 'active'
            private_ip = port.privateIpAddress()
            public_ip = port.publicIpAddress()
            assert private_ip.address == c.primaryIpAddress
            assert public_ip.address == host_ip
            assert port.id in [x.id for x in private_ip.privatePorts()]
            assert port.id in [x.id for x in public_ip.publicPorts()]
            assert port.id not in [x.id for x in public_ip.privatePorts()]
            assert port.id not in [x.id for x in private_ip.publicPorts()]
    finally:
        context.delete(c)


def test_container_port_start(client, context):
    image_uuid = context.image_uuid
    c = client.create_container(imageUuid=image_uuid,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

    try:
        assert c.state == 'creating'
        c = client.wait_success(c)

        assert c.state == 'running'

        ports = c.ports()
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
    finally:
        if c is not None:
            client.wait_success(client.delete(c))


def test_container_port_stop(client, context):
    image_uuid = context.image_uuid
    c = client.create_container(imageUuid=image_uuid,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])
    try:
        assert c.state == 'creating'
        c = client.wait_success(c)
        assert c.state == 'running'

        c = client.wait_success(c.stop())
        assert c.state == 'stopped'

        ports = c.ports()
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
    finally:
        context.delete(c)


def test_container_port_purge(client, context):
    image_uuid = context.image_uuid
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

    assert len(c.ports()) == 3

    c = client.wait_success(c.purge())
    assert c.state == 'purged'

    count = 0
    assert len(c.ports()) == 3

    for port in c.ports():
        assert port.state == 'removed'
        assert port.privateIpAddressId is None
        assert port.publicIpAddressId is None
        if port.privatePort == 80:
            count += 1
            assert port.publicPort is None
        elif port.privatePort == 81:
            count += 1
            assert port.publicPort == 8081
        elif port.privatePort == 82:
            count += 1
            assert port.publicPort == 8082


def test_port_validation(client, context):
    try:
        client.create_container(imageUuid=context.image_uuid,
                                ports=['a'])
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'PortWrongFormat'


def test_ports_service(super_client, client, context):
    c = context.create_container(ports=['80'])

    try:
        agent = super_client.reload(c).hosts()[0].agent()
        assert agent is not None

        items = [x.name for x in agent.configItemStatuses()]

        assert 'host-iptables' in items
        assert 'host-routes' in items

        item = None
        for x in agent.configItemStatuses():
            if x.name == 'host-iptables':
                item = x
                break

        assert item is not None

        port = c.ports()[0]

        assert port.publicPort is None

        port = client.update(port, publicPort=12345)
        assert port.state == 'updating-active'
        assert port.publicPort == 12345

        port = client.wait_success(port)
        assert port.state == 'active'

        new_item = super_client.reload(item)
        assert new_item.requestedVersion > item.requestedVersion
    finally:
        context.delete(c)
