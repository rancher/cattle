from common_fixtures import *  # NOQA


def _clean_clusterhostmap_for_host(host):
    for cluster in host.clusters():
        cluster.removehost(hostId=str(host.id))


def _resource_is_inactive(resource):
    return resource.state == 'inactive'


def _resource_is_active(resource):
    return resource.state == 'active'


# @pytest.mark.skipif('True')
def test_cluster_add_remove_host_actions(super_client, new_context):
    host1 = super_client.reload(new_context.host)
    account = new_context.project
    _clean_clusterhostmap_for_host(host1)

    cluster = super_client.create_cluster(
        accountId=account.id,
        name='testcluster1', port=9000)

    cluster = wait_for_condition(
        super_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    # Add one host to cluster
    cluster = cluster.addhost(hostId=str(host1.id))
    cluster = wait_for_condition(
        super_client, cluster,
        lambda x: len(x.hosts()) == 1,
        lambda x: 'Number of hosts in cluster is: ' + len(x.hosts()))

    assert cluster.hosts()[0].id == host1.id
    assert len(host1.clusters()) == 1
    assert host1.clusters()[0].id == cluster.id

    # activate cluster
    cluster.activate()
    cluster = wait_for_condition(
        super_client, cluster, _resource_is_active,
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
        super_client, cluster,
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
    host2 = register_simulated_host(new_context)

    cluster = cluster.addhost(hostId=str(host2.id))

    cluster = wait_for_condition(
        super_client, cluster,
        lambda x: len(x.hosts()) == 2,
        lambda x: 'Number of hosts in cluster is: ' + len(x.hosts()))

    # Remove 2nd host from cluster
    cluster = cluster.removehost(hostId=str(host2.id))

    cluster = wait_for_condition(
        super_client, cluster,
        lambda x: len(x.hosts()) == 1,
        lambda x: len(x.hosts()))


# temporarily skipping since this was inadvertently deleting the
# real host causing downstream TFs
# @pytest.mark.skipif('True')
def test_host_purge(super_client, new_context):
    host1 = super_client.reload(new_context.host)
    _clean_clusterhostmap_for_host(host1)

    cluster = super_client.create_cluster(
        accountId=new_context.project.id,
        name='testcluster2', port=9000)
    cluster = wait_for_condition(
        super_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    cluster = cluster.addhost(hostId=str(host1.id))
    host1 = super_client.wait_success(host1.deactivate())
    host1 = super_client.wait_success(super_client.delete(host1))
    super_client.wait_success(host1.purge())

    wait_for_condition(
        super_client, cluster, lambda x: len(x.hosts()) == 0)


# @pytest.mark.skipif('True')
def test_cluster_purge(super_client, new_context):
    host1 = super_client.reload(new_context.host)
    _clean_clusterhostmap_for_host(host1)

    cluster = super_client.create_cluster(
        accountId=new_context.project.id,
        name='testcluster3', port=9000)
    cluster = wait_for_condition(
        super_client, cluster, _resource_is_inactive,
        lambda x: 'State is: ' + x.state)

    cluster = cluster.addhost(hostId=str(host1.id))
    cluster = wait_for_condition(
        super_client, cluster, lambda x: len(x.hosts()) == 1)

    cluster.activate()
    cluster = wait_for_condition(
        super_client, cluster, _resource_is_active,
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
    cluster = super_client.wait_success(cluster.deactivate())
    cluster = super_client.wait_success(super_client.delete(cluster))
    cluster = super_client.wait_success(cluster.purge())

    # check no hosts is registered to this cluster
    wait_for_condition(
        super_client, cluster, lambda x: len(x.hosts()) == 0)

    # verify that the agent is removed
    agents = super_client.list_agent(uri=uri)
    wait_for_condition(
        super_client, agents[0],
        lambda x: x.state == 'removed',
        lambda x: 'State is: ' + x.state)

    # verify that the agent instance is removed as well
    agent_instances = super_client.list_instance(agentId=agentId)
    wait_for_condition(
        super_client, agent_instances[0],
        lambda x: x.state == 'removed',
        lambda x: 'State is: ' + x.state)


# @pytest.mark.skipif('True')
def test_cluster_actions_invalid_host_ref(super_client, new_context):
    host1 = super_client.reload(new_context.host)
    _clean_clusterhostmap_for_host(host1)

    cluster = super_client.create_cluster(
        accountId=new_context.project.id,
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
