from common_fixtures import *  # NOQA


def test_link_create(admin_client, sim_context):
    target1 = create_sim_container(admin_client, sim_context)
    target2 = create_sim_container(admin_client, sim_context)

    c = create_sim_container(admin_client, sim_context, instanceLinks={
        'target1_link': target1.id,
        'target2_link': target2.id,
    })

    assert len(c.instanceLinks()) == 2
    assert len(target1.targetInstanceLinks()) == 1
    assert len(target2.targetInstanceLinks()) == 1

    links = c.instanceLinks()
    names = set([x.linkName for x in links])
    assert names == set(['target1_link', 'target2_link'])

    for link in links:
        assert link.state == 'active'

        if link.linkName == 'target1_link':
            assert link.instanceId == c.id
            assert link.targetInstanceId == target1.id
        if link.linkName == 'target2_link':
            assert link.instanceId == c.id
            assert link.targetInstanceId == target2.id


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
