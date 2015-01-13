from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def link_network(admin_client, sim_context):
    nsp = create_agent_instance_nsp(admin_client, sim_context)
    create_and_activate(admin_client, 'linkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return admin_client.by_id_network(nsp.networkId)


def test_link_instance_stop_start(admin_client, sim_context, link_network):
    target1 = create_sim_container(admin_client, sim_context,
                                   ports=['180', '122/udp'],
                                   networkIds=[link_network.id])
    target2 = create_sim_container(admin_client, sim_context,
                                   ports=['280', '222/udp'])

    c = create_sim_container(admin_client, sim_context,
                             networkIds=[link_network.id],
                             instanceLinks={
                                 'target1_link': target1.id,
                                 'target2_link': target2.id})

    assert c.state == 'running'

    ports = set()

    for link in c.instanceLinks():
        for port in link.data.fields.ports:
            ports.add('{}:{}'.format(port.publicPort, port.privatePort))

    assert len(ports) > 0

    new_ports = set()
    c = admin_client.wait_success(c.stop())
    assert c.state == 'stopped'

    for link in c.instanceLinks():
        assert len(link.data.fields.ports) == 2
        for port in link.data.fields.ports:
            new_ports.add('{}:{}'.format(port.publicPort, port.privatePort))

    assert ports == new_ports

    new_ports = set()
    c = admin_client.wait_success(c.start())
    assert c.state == 'running'

    for link in c.instanceLinks():
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


def test_link_create(admin_client, sim_context, link_network):
    target1 = create_sim_container(admin_client, sim_context,
                                   ports=['180', '122/udp'],
                                   networkIds=[link_network.id])
    target2 = create_sim_container(admin_client, sim_context,
                                   ports=['280', '222/udp'])

    c = create_sim_container(admin_client, sim_context,
                             networkIds=[link_network.id],
                             instanceLinks={
                                 'target1_link': target1.id,
                                 'target2_link': target2.id})

    nsp = None
    for test_nsp in link_network.networkServiceProviders():
        if test_nsp.kind == 'agentInstanceProvider':
            nsp = test_nsp

    assert nsp is not None

    assert len(c.instanceLinks()) == 2
    assert len(target1.targetInstanceLinks()) == 1
    assert len(target2.targetInstanceLinks()) == 1

    links = c.instanceLinks()
    names = set([x.linkName for x in links])
    assert names == set(['target1_link', 'target2_link'])

    for link in links:
        assert link.state == 'active'
        assert len(resource_pool_items(admin_client, link)) == 2
        assert link.instanceId == c.id
        ip_address = _find_agent_instance_ip(nsp, c)

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

    c = admin_client.wait_success(c.stop())
    for link in c.instanceLinks():
        assert len(resource_pool_items(admin_client, link)) == 2

    c = admin_client.wait_success(c.remove())
    for link in c.instanceLinks():
        assert len(resource_pool_items(admin_client, link)) == 2

    c = admin_client.wait_success(c.purge())
    for link in c.instanceLinks():
        assert len(link.data.fields.ports) > 0
        assert len(resource_pool_items(admin_client, link)) == 2

    for link in c.instanceLinks():
        link = admin_client.wait_success(link.purge())
        assert len(link.data.fields.ports) == 0
        assert len(resource_pool_items(admin_client, link)) == 0


def test_link_update(admin_client, sim_context):
    target1 = create_sim_container(admin_client, sim_context)
    target2 = create_sim_container(admin_client, sim_context)

    c = create_sim_container(admin_client, sim_context, instanceLinks={
        'target1_link': target1.id,
    })

    link = c.instanceLinks()[0]
    assert link.targetInstanceId == target1.id

    link.targetInstanceId = target2.id
    link = admin_client.update(link, link)
    assert link.state == 'updating-active'

    link = admin_client.wait_success(link)
    assert link.targetInstanceId == target2.id
    assert link.state == 'active'


def test_link_remove_restore(admin_client, sim_context):
    target1 = create_sim_container(admin_client, sim_context)

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      startOnCreate=False,
                                      instanceLinks={
                                          'target1_link': target1.id})
    c = admin_client.wait_success(c)

    links = c.instanceLinks()
    assert len(links) == 1
    link = links[0]

    assert link.state == 'inactive'

    c = admin_client.wait_success(c.start())
    link = admin_client.reload(link)
    assert c.state == 'running'
    assert link.state == 'active'

    c = admin_client.wait_success(c.stop())
    link = admin_client.reload(link)
    assert c.state == 'stopped'
    assert link.state == 'inactive'

    c = admin_client.wait_success(admin_client.delete(c))
    link = admin_client.reload(link)
    assert c.state == 'removed'
    assert link.state == 'inactive'

    c = admin_client.wait_success(c.restore())
    link = admin_client.reload(link)
    assert c.state == 'stopped'
    assert link.state == 'inactive'

    c = admin_client.wait_success(admin_client.delete(c))
    link = admin_client.reload(link)
    assert c.state == 'removed'
    assert link.state == 'inactive'

    c = admin_client.wait_success(c.purge())
    link = admin_client.reload(link)
    assert c.state == 'purged'
    assert link.state == 'removed'


def test_null_links(admin_client, sim_context):
    c = create_sim_container(admin_client, sim_context, instanceLinks={
        'null_link': None
    })

    links = c.instanceLinks()
    assert len(links) == 1

    assert links[0].state == 'active'
    assert links[0].linkName == 'null_link'
    assert links[0].targetInstanceId is None


def test_link_timeout(admin_client, sim_context, link_network):
    t = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      startOnCreate=False)

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      networkIds=[link_network.id],
                                      instanceLinks={'t': t.id},
                                      data={'linkWaitTime': 100})

    c = admin_client.wait_transitioning(c)

    msg = 'Timeout waiting for instance link t'
    assert c.state == 'removed'
    assert c.transitioning == 'error'
    assert c.transitioningMessage == '{} : {}'.format(msg, msg)
