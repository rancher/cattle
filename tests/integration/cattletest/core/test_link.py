from common_fixtures import *  # NOQA


def test_link_instance_stop_start(super_client, client, context):
    target1 = context.create_container(ports=['180', '122/udp'])
    target2 = context.create_container(ports=['280', '222/udp'])

    c = context.create_container(instanceLinks={
        'target1_link': target1.id,
        'target2_link': target2.id})

    assert c.state == 'running'
    assert len(c.instanceLinks()) > 0


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
    assert link.removed is not None


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

    assert c.state == 'running'


def test_link_remove_instance_restart(client, super_client, context):
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
    assert c.state == 'stopped'

    link = client.reload(link)

    link = super_client.wait_success(link.remove())
    assert link.state == 'removed'

    c = client.wait_success(c.start())
    assert c.state == 'running'
