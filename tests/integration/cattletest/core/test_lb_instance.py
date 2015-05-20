from common_fixtures import *  # NOQA
from cattle import ApiError


def _process_names(processes):
    return set([x.processName for x in processes])


@pytest.fixture(scope='session')
def config_id(client):
    default_lb_config = client. \
        create_loadBalancerConfig(name=random_str())
    default_lb_config = client.wait_success(default_lb_config)
    return default_lb_config.id


def test_add_host_to_lb(client, super_client, context, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # verify the mapping
    _validate_add_host(host, lb, client)

    # verify that the instance is set with requestedHost
    # and instance_triggered_stop flags
    host_id = get_plain_id(super_client, host)
    assert host_id == '{}'.format(instance.data.fields.requestedHostId)
    assert instance.instanceTriggeredStop == 'restart'


def test_add_host_twice(client, context, config_id):
    host = context.host

    lb = _create_valid_lb(client, config_id)

    lb.addhost(hostId=host.id)
    _validate_add_host(host, lb, client)

    with pytest.raises(ApiError) as e:
        lb.addhost(hostId=host.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'
    assert e.value.error.fieldName == 'hostId'


def test_remove_non_existing_host(new_context):
    client = new_context.client
    config = client. \
        create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    host = new_context.host
    host1 = register_simulated_host(new_context)

    lb = _create_valid_lb(client, config.id)

    lb.addhost(hostId=host.id)
    _validate_add_host(host, lb, client)

    with pytest.raises(ApiError) as e:
        lb.removehost(hostId=host1.id)

    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidOption'
    assert e.value.error.fieldName == 'hostId'


def test_delete_host_from_lb(client, context, super_client, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_instance_stopped(client, context,
                                                   super_client, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # stop the lb instance
    instance = client.wait_success(instance)
    if instance.state == 'running':
        instance = client.wait_success(instance.stop())
        assert instance.state == 'stopped'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_instance_removed(client, context,
                                                   super_client, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # stop the lb instance
    instance = client.wait_success(instance)
    instance = client.wait_success(instance.stop())
    assert instance.state == 'stopped'

    # remove the lb instance
    instance = client.wait_success(instance.remove())
    assert instance.state == 'removed'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_agent_inactive(client, context,
                                                 super_client, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # deactivate the agent
    agent = super_client.wait_success(agent.deactivate())
    assert agent.state == 'inactive'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent)


def test_delete_host_from_lb_when_agent_removed(client, context,
                                                super_client, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # deactivate the agent
    agent = super_client.wait_success(agent.deactivate())
    assert agent.state == 'inactive'

    # remove the agent
    agent = super_client.wait_success(agent.remove())
    assert agent.state == 'removed'

    # remove the host from lb
    lb.removehost(hostId=host.id)

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent)


def test_delete_lb(client, super_client, context, config_id):
    host = context.host

    agent, lb, uri, instance = _create_lb_w_host(client,
                                                 config_id, host,
                                                 super_client)

    # remove the lb
    super_client.delete(lb)
    lb = super_client.wait_success(lb)
    assert lb.state == 'removed'

    # verify the cleanup was executed
    _verify_host_map_cleanup(client, host, lb,
                             super_client, uri, agent)


def test_set_hosts(new_context):
    client = new_context.client
    config = client. \
        create_loadBalancerConfig(name=random_str())
    config = client.wait_success(config)

    host1 = new_context.host
    host2 = register_simulated_host(new_context)

    lb = _create_valid_lb(client, config.id)

    # 1. Set hosts with 2 lbs
    lb = lb.sethosts(hostIds=[host1.id, host2.id])
    lb = client.wait_success(lb)

    # VERIFICATION FOR HOST1
    # verify the mapping
    _validate_add_host(host1, lb, client)

    # VERIFICATION FOR HOST2
    # verify the mapping
    _validate_add_host(host2, lb, client)

    # 2. Remove the host from lb
    lb = lb.removehost(hostId=host1.id)

    # verify the cleanup was executed
    validate_remove_host(host1, lb, client)

    # 3. Re-add the host again
    lb = lb.sethosts(hostIds=[host1.id, host2.id])
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host1.id)

    assert len(host_maps) == 2
    if host_maps[0].state != 'removed':
        host_map = host_maps[0]
    else:
        host_map = host_maps[1]

    wait_for_condition(
        client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _create_valid_lb(client, config_id):
    test_lb = client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config_id)
    test_lb = client.wait_success(test_lb)
    return test_lb


def _create_lb_w_host(client, config_id, host, super_client):
    # create lb
    lb = _create_valid_lb(client, config_id)

    # add host to lb
    lb.addhost(hostId=host.id)
    host_map = _validate_add_host(host, lb, client)

    # verify that the agent got created
    uri = 'delegate:///?lbId={}&hostMapId={}'. \
        format(get_plain_id(super_client, lb),
               get_plain_id(super_client, host_map))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1

    # verify that the agent instance got created
    agent_instances = super_client.list_instance(agentId=agents[0].id)
    assert len(agent_instances) == 1

    agent = agents[0]
    instance = agent_instances[0]
    return agent, lb, uri, instance


def _verify_host_map_cleanup(client, host,
                             lb, super_client, uri, agent):
    # verify the mapping is gone
    validate_remove_host(host, lb, client)

    # verify that the lb instance is gone
    instances = super_client.list_instance(agentId=agent.id)
    if not instances:
        instance = client.wait_success(instances[0])
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


def validate_remove_host(host, lb, client):
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        client, host_map, _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    return host_map


def _add_listener_to_config(client, config):
    listener = client.create_loadBalancerListener(name=random_str(),
                                                  sourcePort='8080',
                                                  targetPort='80',
                                                  sourceProtocol='http',
                                                  targetProtocol='tcp')
    listener = client.wait_success(listener)
    config = config.addlistener(loadBalancerListenerId=listener.id)
    _validate_add_listener(config, listener, client)
    return config, listener


def test_validate_ports(client, super_client, context):
    host = context.host

    # create config with 1 listener
    config = client. \
        create_loadBalancerConfig(name=random_str())
    client.wait_success(config)
    config, listener = _add_listener_to_config(client, config)

    # create load balancer
    lb = client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config.id)
    lb = client.wait_success(lb)

    # add host to lb
    lb.addhost(hostId=host.id)

    # verify that the lb instance/host_map got created
    host_map = _validate_add_host(host, lb, client)
    uri = 'delegate:///?lbId={}&hostMapId={}'. \
        format(get_plain_id(super_client, lb),
               get_plain_id(super_client, host_map))
    agents = super_client.list_agent(uri=uri)
    assert len(agents) == 1

    # verify that the agent instance got created
    agent_instances = super_client.list_instance(agentId=agents[0].id)
    assert len(agent_instances) == 1
    instance = agent_instances[0]

    # verify that the instance is set with Ports attribute
    ports = super_client.list_port(publicPort='8080', instanceId=instance.id)
    assert len(ports) == 1

    # remove listener; make sure that the port was removed
    listener = client.wait_success(client.delete(listener))
    assert listener.state == 'removed'
    ports = super_client.list_port(publicPort='8080', instanceId=instance.id)
    assert len(ports) == 1
    wait_for_condition(
        super_client, ports[0], _resource_is_removed,
        lambda x: 'State is: ' + x.state)

    # restart the instance and verify that the port didn't appear again
    instance = client.wait_success(instance.stop())
    assert instance.state == 'stopped'
    instance = client.wait_success(instance.start())
    assert instance.state == 'running'
    ports = super_client.list_port(publicPort='8080', instanceId=instance.id)
    assert len(ports) == 1
    assert ports[0].removed is not None

    # re-add the listener, add target to lb
    # and verify that the port is back in
    image_uuid = context.image_uuid
    container = client.create_container(imageUuid=image_uuid)
    container = client.wait_success(container)
    lb = lb.addtarget(instanceId=container.id)
    _validate_add_target(container, lb, client)
    _add_listener_to_config(client, config)
    ports = super_client.list_port(publicPort='8080', instanceId=instance.id)
    assert len(ports) == 2


def _validate_add_listener(config, listener, client):
    lb_config_maps = client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=listener.id,
                                           loadBalancerConfigId=config.id)
    assert len(lb_config_maps) == 1
    config_map = lb_config_maps[0]
    wait_for_condition(
        client, config_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed' or resource.removed is not None


def _validate_add_host(host, lb, client):
    host_maps = client. \
        list_loadBalancerHostMap(loadBalancerId=lb.id,
                                 hostId=host.id)
    assert len(host_maps) == 1
    host_map = host_maps[0]
    wait_for_condition(
        client, host_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
    assert host_map.hostId == host.id
    return host_map


def _validate_add_target(container1, lb, client):
    target_maps = client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)
