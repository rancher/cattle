from common_fixtures import *  # NOQA


def test_link_instance_stop_start(super_client, client, context):
    target1 = context.create_container(ports=['180', '122/udp'])
    target2 = context.create_container(ports=['280', '222/udp'])

    c = context.create_container(instanceLinks={
        'target1_link': target1.id,
        'target2_link': target2.id})

    assert c.state == 'running'

    ports = set()

    for link in c.instanceLinks():
        for port in super_client.reload(link).data.fields.ports:
            ports.add('{}:{}'.format(port.publicPort, port.privatePort))

    assert len(ports) > 0

    new_ports = set()
    c = client.wait_success(c.stop())
    assert c.state == 'stopped'

    for link in super_client.reload(c).instanceLinks():
        assert len(link.data.fields.ports) == 2
        for port in link.data.fields.ports:
            new_ports.add('{}:{}'.format(port.publicPort, port.privatePort))

    assert ports == new_ports

    new_ports = set()
    c = client.wait_success(c.start())
    assert c.state == 'running'

    for link in super_client.reload(c).instanceLinks():
        assert len(link.data.fields.ports) == 2
        for port in link.data.fields.ports:
            new_ports.add('{}:{}'.format(port.publicPort, port.privatePort))

    assert ports == new_ports


def _find_agent_instance_ip(nsp, source):
    assert source is not None
    vnet_id = source.nics()[0].vnetId
    assert vnet_id is not None

    for agent_instance in nsp.instances():
        if agent_instance.nics()[0].vnetId == vnet_id:
            assert agent_instance.primaryIpAddress is not None
            return agent_instance.primaryIpAddress

    assert False, 'Failed to find agent instance for ' + source.id


def test_link_create(client, super_client, context):
    target1 = context.create_container(ports=['180', '122/udp'])
    target2 = context.create_container(ports=['280', '222/udp'])

    c = context.create_container(instanceLinks={
        'target1_link': target1.id,
        'target2_link': target2.id})

    assert c.state == 'running'

    assert len(c.instanceLinks()) == 2
    assert len(target1.targetInstanceLinks()) == 1
    assert len(target2.targetInstanceLinks()) == 1

    links = c.instanceLinks()
    names = set([x.linkName for x in links])
    assert names == set(['target1_link', 'target2_link'])

    for link in links:
        link = super_client.reload(link)
        assert link.state == 'active'
        assert len(resource_pool_items(super_client, link)) == 2
        assert link.instanceId == c.id
        ip_address = _find_agent_instance_ip(context.nsp,
                                             super_client.reload(c))

        if link.linkName == 'target1_link':
            assert link.targetInstanceId == target1.id
            assert len(link.data.fields.ports) == 2
            for port in link.data.fields.ports:
                assert port.ipAddress == ip_address
                assert port.publicPort is not None
                if port.privatePort == 180:
                    assert port.protocol == 'tcp'
                elif port.privatePort == 122:
                    assert port.protocol == 'udp'
                else:
                    assert False

        if link.linkName == 'target2_link':
            assert link.targetInstanceId == target2.id
            assert len(link.data.fields.ports) == 2
            for port in link.data.fields.ports:
                assert port.ipAddress == ip_address
                assert port.publicPort is not None
                if port.privatePort == 280:
                    assert port.protocol == 'tcp'
                elif port.privatePort == 222:
                    assert port.protocol == 'udp'
                else:
                    assert False

    c = client.wait_success(c.stop())
    for link in c.instanceLinks():
        assert len(resource_pool_items(super_client, link)) == 2

    c = client.wait_success(c.remove())
    for link in c.instanceLinks():
        assert len(resource_pool_items(super_client, link)) == 2

    c = client.wait_success(c.purge())
    for link in super_client.reload(c).instanceLinks():
        assert len(link.data.fields.ports) > 0
        assert len(resource_pool_items(super_client, link)) == 2

    for link in c.instanceLinks():
        link = super_client.wait_success(link.purge())
        assert len(link.data.fields.ports) == 0
        assert len(resource_pool_items(super_client, link)) == 0


def test_link_update(client, context):
    target1 = context.create_container()
    target2 = context.create_container()

    c = context.create_container(instanceLinks={
        'target1_link': target1.id,
    })

    link = c.instanceLinks()[0]
    assert link.targetInstanceId == target1.id

    link.targetInstanceId = target2.id
    link = client.update(link, link)
    assert link.state == 'updating-active'

    link = client.wait_success(link)
    assert link.targetInstanceId == target2.id
    assert link.state == 'active'


def test_link_remove_restore(client, context):
    target1 = context.create_container()

    c = client.create_container(imageUuid=context.image_uuid,
                                startOnCreate=False,
                                instanceLinks={
                                    'target1_link': target1.id})
    c = client.wait_success(c)

    links = c.instanceLinks()
    assert len(links) == 1
    link = links[0]

    assert link.state == 'inactive'

    c = client.wait_success(c.start())
    link = client.reload(link)
    assert c.state == 'running'
    assert link.state == 'active'

    c = client.wait_success(c.stop())
    link = client.reload(link)
    assert c.state == 'stopped'
    assert link.state == 'inactive'

    c = client.wait_success(client.delete(c))
    link = client.reload(link)
    assert c.state == 'removed'
    assert link.state == 'inactive'

    c = client.wait_success(c.restore())
    link = client.reload(link)
    assert c.state == 'stopped'
    assert link.state == 'inactive'

    c = client.wait_success(client.delete(c))
    link = client.reload(link)
    assert c.state == 'removed'
    assert link.state == 'inactive'

    c = client.wait_success(c.purge())
    link = client.reload(link)
    assert c.state == 'purged'
    assert link.state == 'removed'


def test_null_links(context):
    c = context.create_container(instanceLinks={
        'null_link': None
    })

    links = c.instanceLinks()
    assert len(links) == 1

    assert links[0].state == 'active'
    assert links[0].linkName == 'null_link'
    assert links[0].targetInstanceId is None


def test_link_timeout(super_client, client, context):
    t = client.create_container(imageUuid=context.image_uuid,
                                startOnCreate=False)

    c = super_client.create_container(accountId=context.project.id,
                                      imageUuid=context.image_uuid,
                                      instanceLinks={'t': t.id},
                                      data={'linkWaitTime': 100})

    c = client.wait_transitioning(c)

    msg = 'Timeout waiting for instance link t'
    assert c.state == 'removed'
    assert c.transitioning == 'error'
    assert c.transitioningMessage == '{} : {}'.format(msg, msg)
