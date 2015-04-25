from common_fixtures import *  # NOQA


def _clean_clusterhostmap_for_host(host):
    for cluster in host.clusters():
        cluster.removehost(hostId=str(host.id))


def _resource_is_inactive(resource):
    return resource.state == 'inactive'


def _resource_is_active(resource):
    return resource.state == 'active'


def test_cluster_add_remove_host_actions(admin_client, super_client):
    sim_context = create_sim_context(
        super_client, 'simagent' + random_str(), ip='192.168.10.15')
    sim_context2 = create_sim_context(
        super_client, 'simagent' + random_str(), ip='192.168.10.16')
    host1 = sim_context['host']
    _clean_clusterhostmap_for_host(host1)

    create_agent_instance_nsp(super_client, sim_context)

    cluster = admin_client.create_cluster(
        name='testcluster1', port=9000)

    cluster = wait_for_condition(
        admin_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    # Add one host to cluster
    cluster = cluster.addhost(hostId=str(host1.id))
    cluster = wait_for_condition(
        admin_client, cluster,
        lambda x: len(x.hosts()) == 1,
        lambda x: 'Number of hosts in cluster is: ' + len(x.hosts()))

    assert cluster.hosts()[0].id == host1.id
    assert len(host1.clusters()) == 1
    assert host1.clusters()[0].id == cluster.id

    # activate cluster
    cluster.activate()
    cluster = wait_for_condition(
        admin_client, cluster, _resource_is_active,
        lambda x: 'State is: ' + x.state)

    # verify that the agent got created
    uri = 'sim:///?clusterId={}&managingHostId={}'. \
        format(get_plain_id(super_client, cluster),
               get_plain_id(super_client, host1))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1

    # verify that the agent instance got created
    agent_instances = super_client.list_instance(agentId=agents[0].id)
    assert len(agent_instances) == 1

    try:
        cluster.addhost(hostId=str(host1.id))
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    cluster = cluster.removehost(hostId=str(host1.id))
    cluster = wait_for_condition(
        admin_client, cluster,
        lambda x: len(x.hosts()) == 0,
        lambda x: 'Number of hosts in cluster is: ' + len(x.hosts()))

    try:
        cluster = cluster.removehost(hostId=str(host1.id))
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    cluster = cluster.addhost(hostId=str(host1.id))
    assert len(cluster.hosts()) == 1

    # Add 2nd host to cluster
    host2 = sim_context2['host']

    cluster = cluster.addhost(hostId=str(host2.id))

    cluster = wait_for_condition(
        admin_client, cluster,
        lambda x: len(x.hosts()) == 2,
        lambda x: 'Number of hosts in cluster is: ' + len(x.hosts()))

    # Remove 2nd host from cluster
    cluster = cluster.removehost(hostId=str(host2.id))

    cluster = wait_for_condition(
        admin_client, cluster,
        lambda x: len(x.hosts()) == 1,
        lambda x: len(x.hosts()))


def test_host_purge(admin_client, super_client, new_sim_context):
    host1 = new_sim_context['host']
    _clean_clusterhostmap_for_host(host1)

    cluster = admin_client.create_cluster(
        name='testcluster2', port=9000)
    cluster = wait_for_condition(
        admin_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    cluster = cluster.addhost(hostId=str(host1.id))

    host1 = admin_client.wait_success(host1.deactivate())
    host1 = admin_client.wait_success(admin_client.delete(host1))
    admin_client.wait_success(host1.purge())

    wait_for_condition(
        admin_client, cluster, lambda x: len(x.hosts()) == 0)


def test_cluster_purge(admin_client, super_client, new_sim_context):
    host1 = new_sim_context['host']
    _clean_clusterhostmap_for_host(host1)

    create_agent_instance_nsp(super_client, new_sim_context)

    cluster = admin_client.create_cluster(
        name='testcluster3', port=9000)
    cluster = wait_for_condition(
        admin_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    cluster = cluster.addhost(hostId=str(host1.id))
    cluster = wait_for_condition(
        admin_client, cluster, lambda x: len(x.hosts()) == 1)

    cluster.activate()
    cluster = wait_for_condition(
        admin_client, cluster, _resource_is_active,
        lambda x: 'State is: ' + x.state)

    # verify that the agent got created
    uri = 'sim:///?clusterId={}&managingHostId={}'. \
        format(get_plain_id(super_client, cluster),
               get_plain_id(super_client, host1))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1

    # verify that the agent instance got created
    agentId = agents[0].id
    agent_instances = super_client.list_instance(agentId=agentId)
    assert len(agent_instances) == 1

    # deactivate, remove, and purge cluster
    cluster = admin_client.wait_success(cluster.deactivate())
    cluster = admin_client.wait_success(admin_client.delete(cluster))
    cluster = admin_client.wait_success(cluster.purge())

    # check no hosts is registered to this cluster
    wait_for_condition(
        admin_client, cluster, lambda x: len(x.hosts()) == 0)

    # verify that the agent is removed
    agents = super_client.list_agent(uri=uri)
    wait_for_condition(
        admin_client, agents[0],
        lambda x: x.state == 'removed',
        lambda x: 'State is: ' + x.state)

    # verify that the agent instance is removed as well
    agent_instances = super_client.list_instance(agentId=agentId)
    wait_for_condition(
        admin_client, agent_instances[0],
        lambda x: x.state == 'removed',
        lambda x: 'State is: ' + x.state)


def test_cluster_actions_invalid_host_ref(admin_client, super_client,
                                          new_sim_context):
    host1 = new_sim_context['host']
    _clean_clusterhostmap_for_host(host1)

    cluster = admin_client.create_cluster(
        name='testcluster4', port=9000)

    try:
        cluster.addhost(hostId='badvalue')
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    try:
        cluster.removehost(hostId='badvalue')
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'
