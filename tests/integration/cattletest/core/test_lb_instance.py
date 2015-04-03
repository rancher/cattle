from common_fixtures import *  # NOQA
from cattle import ApiError


def _process_names(processes):
    return set([x.processName for x in processes])


@pytest.fixture(scope='session')
def config_id(super_client):
    default_lb_config = super_client. \
        create_loadBalancerConfig(name=random_str())
    default_lb_config = super_client.wait_success(default_lb_config)
    return default_lb_config.id


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return nsp


def validate_add_host(host, lb, super_client):
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host.id


def test_add_host_to_lb(admin_client, super_client, sim_context,
                        config_id, nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # verify the mapping
    validate_add_host(host, lb, super_client)

    # verify that the instance is set with requestedHost
    # and instance_triggered_stop flags
    host_id = get_plain_id(super_client, host)
    assert host_id == '{}'.format(instance.data.fields.requestedHostId)
    assert instance.instanceTriggeredStop == 'restart'


def test_add_host_twice(admin_client, super_client, sim_context,
                        config_id, nsp):
    host = sim_context['host']

    lb = _create_valid_lb(super_client, sim_context, config_id, nsp)

    lb.addhost(hostId=host.id)
    validate_add_host(host, lb, super_client)

    with pytest.raises(ApiError) as e:
        lb.addhost(hostId=host.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'hostId'


def test_remove_non_existing_host(admin_client, super_client, sim_context,
                                  sim_context2, config_id, nsp):
    host = sim_context['host']
    host1 = sim_context2['host']

    lb = _create_valid_lb(super_client, sim_context, config_id, nsp)

    lb.addhost(hostId=host.id)
    validate_add_host(host, lb, super_client)

    with pytest.raises(ApiError) as e:
        lb.removehost(hostId=host1.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'hostId'


def test_delete_host_from_lb(admin_client,
                             sim_context, super_client,
                             config_id, nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_instance_stopped(admin_client,
                                                   sim_context,
                                                   super_client,
                                                   config_id,
                                                   nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # stop the lb instance
    instance = wait_success(super_client, instance)
    if instance.state == 'running':
        instance = wait_success(super_client, instance.stop())
        assert instance.state == 'stopped'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_instance_removed(admin_client,
                                                   sim_context,
                                                   super_client,
                                                   config_id,
                                                   nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # stop the lb instance
    instance = wait_success(super_client, instance)
    instance = wait_success(super_client, instance.stop())
    assert instance.state == 'stopped'

    # remove the lb instance
    instance = wait_success(super_client, instance.remove())
    assert instance.state == 'removed'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_agent_inactive(admin_client,
                                                 sim_context,
                                                 super_client,
                                                 config_id,
                                                 nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # deactivate the agent
    agent = wait_success(super_client, agent.deactivate())
    assert agent.state == 'inactive'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_agent_removed(admin_client,
                                                sim_context,
                                                super_client,
                                                config_id,
                                                nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # deactivate the agent
    agent = wait_success(super_client, agent.deactivate())
    assert agent.state == 'inactive'

    # remove the agent
    agent = wait_success(super_client, agent.remove())
    assert agent.state == 'removed'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent)


def test_delete_lb(admin_client, super_client, sim_context,
                   config_id, nsp):
    host = sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    # remove the lb
    lb = admin_client.wait_success(admin_client.delete(lb))
    assert lb.state == 'removed'

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host, lb,
                             super_client, uri, agent)


def test_delete_host_alena(admin_client, super_client, new_sim_context,
                           sim_context,
                           config_id, nsp):
    host = new_sim_context['host']

    agent, lb, uri, instance = _create_lb_w_host(admin_client,
                                                 config_id, host,
                                                 sim_context,
                                                 super_client,
                                                 nsp)

    time.sleep(10)

    # remove the host
    host = admin_client.wait_success(host.deactivate())
    assert host.state == 'inactive'
    host = admin_client.wait_success(host.remove())
    assert host.state == 'removed'

    # verify the cleanup was executed
    _verify_host_map_cleanup(admin_client, host, lb,
                             super_client, uri, agent)


def test_set_hosts(admin_client,
                   super_client,
                   sim_context,
                   new_sim_context,
                   config_id, nsp):
    host1 = new_sim_context['host']
    host2 = sim_context['host']

    lb = _create_valid_lb(super_client, sim_context, config_id, nsp)

    # 1. Set hosts with 2 lbs
    lb = lb.sethosts(hostIds=[host1.id, host2.id])
    lb = admin_client.wait_success(lb)

    # VERIFICATION FOR HOST1
    # verify the mapping
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host1.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)

    assert host_map.hostId == host1.id

    # VERIFICATION FOR HOST2
    # verify the mapping
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host2.id)

    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host2.id

    # 2. Remove the host
    # remove the host from lb
    lb = lb.removehost(hostId=host1.id)

    # verify the cleanup was executed
    validate_remove_host(host1, lb, super_client)

    # 3. Re-add the host again
    lb = lb.sethosts(hostIds=[host1.id, host2.id])
    lb = admin_client.wait_success(lb)


def _create_valid_lb(super_client, sim_context, config_id, nsp):
    default_lb_config = super_client. \
        create_loadBalancerConfig(name=random_str())
    super_client.wait_success(default_lb_config)

    im_id = sim_context['imageUuid']
    test_lb = super_client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config_id,
                            loadBalancerInstanceImageUuid=im_id,
                            loadBalancerInstanceUriPredicate='sim://',
                            networkId=nsp.networkId)
    test_lb = super_client.wait_success(test_lb)
    return test_lb


def _create_lb_w_host(admin_client, config_id, host,
                      sim_context, super_client, nsp):
    # create lb
    lb = _create_valid_lb(super_client, sim_context, config_id, nsp)

    # add host to lb
    lb.addhost(hostId=host.id)
    validate_add_host(host, lb, super_client)

    # verify that the agent got created
    uri = 'sim://?lbId={}&hostId={}'. \
        format(get_plain_id(super_client, lb),
               get_plain_id(super_client, host))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1

    # verify that the agent instance got created
    agent_instances = super_client.list_instance(agentId=agents[0].id)
    assert len(agent_instances) == 1

    agent = agents[0]
    instance = agent_instances[0]
    return agent, lb, uri, instance


def _verify_host_map_cleanup(admin_client, host,
                             lb, super_client, uri, agent):
    # verify the mapping is gone
    validate_remove_host(host, lb, super_client)

    # verify that the lb instance is gone
    instances = super_client.list_instance(agentId=agent.id)
    if not instances:
        instance = admin_client.wait_success(instances[0])
        assert instance.state == 'removed'

    # verify that the agent is gone
    _wait_until_agent_removed(uri, super_client)


def _wait_until_agent_removed(uri, super_client, timeout=30):
    # need this function because agent state changes
    # active->deactivating->removed
    start = time.time()
    agent = super_client.list_agent(uri=uri)[0]
    agent = super_client.wait_success(agent)
    while agent.state != 'removed':
        time.sleep(.5)
        agent = super_client.reload(agent)
        if time.time() - start > timeout:
            assert 'Timeout waiting for agent to be removed.'

    return agent


def validate_remove_host(host, lb, super_client):
    host_maps = super_client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        super_client, host_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    return host_map


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'
