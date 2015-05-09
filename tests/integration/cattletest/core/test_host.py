from common_fixtures import *  # NOQA


def test_host_deactivate(admin_user_client, new_sim_context):
    host = new_sim_context['host']
    agent = new_sim_context['agent']

    assert host.state == 'active'
    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'active'

    host = admin_user_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'inactive'


def test_host_deactivate_two_hosts(admin_user_client, super_client,
                                   new_sim_context):
    host = new_sim_context['host']
    agent = new_sim_context['agent']

    assert host.state == 'active'
    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'active'

    # Create another host using the same agent
    other_host = super_client.create_host(agentId=agent.id)
    other_host = super_client.wait_success(other_host)
    assert other_host.state == 'active'
    assert other_host.agentId == agent.id

    host = admin_user_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'active'


def test_host_activate(admin_user_client, new_sim_context):
    host = new_sim_context['host']
    agent = new_sim_context['agent']

    assert host.state == 'active'
    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'active'

    host = admin_user_client.wait_success(host.deactivate())
    assert host.state == 'inactive'

    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'inactive'

    host = admin_user_client.wait_success(host.activate())
    assert host.state == 'active'
    agent = admin_user_client.wait_success(agent)
    assert agent.state == 'active'


def test_host_purge(super_client, new_sim_context):
    image_uuid = 'sim:{}'.format(random_num())
    host = new_sim_context['host']
    agent = new_sim_context['agent']

    assert host.state == 'active'
    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    c1 = super_client.create_container(imageUuid=image_uuid,
                                       requestedHostId=host.id)
    c1 = super_client.wait_success(c1)
    assert c1.state == 'running'

    c2 = super_client.create_container(imageUuid=image_uuid,
                                       requestedHostId=host.id)
    c2 = super_client.wait_success(c2)
    assert c2.state == 'running'

    host = super_client.wait_success(host.deactivate())
    host = super_client.wait_success(super_client.delete(host))
    assert host.state == 'removed'
    assert host.removed is not None

    host = super_client.wait_success(host.purge())
    assert host.state == 'purged'

    c1 = super_client.reload(c1)
    assert c1.removed is not None
    assert c1.state == 'removed'

    c2 = super_client.reload(c2)
    assert c2.removed is not None
    assert c2.state == 'removed'

    c1 = super_client.wait_success(c1.purge())
    assert c1.state == 'purged'

    volume = super_client.wait_success(c1.volumes()[0])
    assert volume.state == 'removed'

    volume = super_client.wait_success(volume.purge())
    assert volume.state == 'purged'
