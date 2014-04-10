from common_fixtures import *  # NOQA
from test_virtual_machine import *  # NOQA


def test_container_port_create_start(client, admin_client,
                                     sim_context, network):
    image_uuid = sim_context['imageUuid']
    host = sim_context['host']
    host_ip = sim_context['hostIp'].address
    assert host_ip is not None

    c = client.create_container(imageUuid=image_uuid,
                                networkIds=[network.id],
                                requestedHostId=host.id,
                                startOnCreate=False,
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

    assert c.state == 'creating'
    c = client.wait_success(c)

    assert c.state == 'stopped'

    c_admin = admin_client.update(c, requestedHostId=host.id)
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
    assert admin_client.reload(c).hosts()[0].id == host.id

    for port in c.ports():
        assert port.state == 'active'
        assert port.privateIpAddress().address == c.primaryIpAddress
        assert port.publicIpAddress().address == host_ip


def test_container_port_start(client, sim_context, network):
    image_uuid = sim_context['imageUuid']
    c = client.create_container(imageUuid=image_uuid,
                                networkIds=[network.id],
                                ports=[
                                    80,
                                    '8081:81',
                                    '8082:82/udp'])

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


def test_container_port_stop(admin_client, sim_context, network):
    host = sim_context['host']
    image_uuid = sim_context['imageUuid']
    c = admin_client.create_container(imageUuid=image_uuid,
                                      requestedHostId=host.id,
                                      networkIds=[network.id],
                                      ports=[
                                          80,
                                          '8081:81',
                                          '8082:82/udp'])

    assert c.state == 'creating'
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    c = admin_client.wait_success(c.stop())
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


def test_container_port_purge(admin_client, sim_context, network):
    image_uuid = sim_context['imageUuid']
    c = admin_client.create_container(imageUuid=image_uuid,
                                      networkIds=[network.id],
                                      ports=[
                                          80,
                                          '8081:81',
                                          '8082:82/udp'])

    assert c.state == 'creating'
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    c = admin_client.wait_success(c.stop(remove=True))
    assert c.state == 'removed'

    assert len(c.ports()) == 3

    c = admin_client.wait_success(c.purge())
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


def test_port_validation(client, sim_context, network):
    try:
        client.create_container(imageUuid=sim_context['imageUuid'],
                                ports=['a'])
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'PortWrongFormat'
