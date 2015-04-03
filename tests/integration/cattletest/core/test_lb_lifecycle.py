from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def nsp(super_client, sim_context):
    nsp = create_agent_instance_nsp(super_client, sim_context)
    create_and_activate(super_client, 'networkService',
                        networkServiceProviderId=nsp.id,
                        networkId=nsp.networkId)

    return nsp


def test_add_lb_w_host_and_target(super_client, admin_client, sim_context,
                                  nsp):
    port = 88
    port1 = 101
    # add host
    agent, lb, uri, instance, config = _create_lb_w_host(super_client,
                                                         admin_client,
                                                         sim_context,
                                                         port,
                                                         nsp)

    # add target to a load balancer
    image_uuid = sim_context['imageUuid']
    container = admin_client.create_container(imageUuid=image_uuid,
                                              startOnCreate=False)
    container = admin_client.wait_success(container)
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    # check the port
    ports = super_client.list_port(publicPort=port, instanceId=instance.id)
    assert len(ports) == 1
    assert ports[0].state == 'active'
    assert ports[0].publicPort == port
    assert ports[0].privatePort == port

    # wait till the instance is up and running
    # instance = admin_client.wait_success(instance)
    # assert instance.state == 'running'

    # add listener to the config
    listener = _create_valid_listener(super_client,
                                      admin_client,
                                      sim_context,
                                      port1)
    # add listener to config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config, listener, super_client)
    # check the port
    ports = super_client.list_port(publicPort=port1, instanceId=instance.id)
    assert len(ports) == 1
    assert ports[0].state == 'active'
    assert ports[0].publicPort == port1
    assert ports[0].privatePort == port1


def test_destroy_lb_instance(super_client, admin_client, sim_context, nsp):
    port = 77
    # add host
    agent, lb, uri, instance, config = _create_lb_w_host(super_client,
                                                         admin_client,
                                                         sim_context,
                                                         port, nsp)
    # add target to a load balancer
    image_uuid = sim_context['imageUuid']
    container = admin_client.create_container(imageUuid=image_uuid,
                                              startOnCreate=False)
    container = admin_client.wait_success(container)
    lb = lb.addtarget(instanceId=container.id)
    validate_add_target(admin_client, container, lb, super_client)

    # destroy the lb instance
    # stop the lb instance
    if instance.state == 'running':
        instance = wait_success(super_client, instance)
        instance = wait_success(super_client, instance.stop())
        assert instance.state == 'stopped'

    # remove the lb instance
    instance = wait_success(super_client, instance.remove())
    assert instance.state == 'removed'

    # check that the port is deactivated
    ports = super_client.list_port(publicPort=port, instanceId=instance.id)
    ports = super_client.list_port(publicPort=port, instanceId=instance.id)
    assert len(ports) == 1
    assert ports[0].state == 'inactive'
    assert ports[0].publicPort == port
    assert ports[0].privatePort == port


def _create_valid_lb(super_client, admin_client, sim_context, listenerPort,
                     nsp):
    config = _create_config(super_client, admin_client, sim_context,
                            listenerPort)
    default_lb_config = super_client. \
        create_loadBalancerConfig(name=random_str())
    super_client.wait_success(default_lb_config)

    im_id = sim_context['imageUuid']
    test_lb = super_client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config.id,
                            loadBalancerInstanceImageUuid=im_id,
                            loadBalancerInstanceUriPredicate='sim://',
                            networkId=nsp.networkId)
    test_lb = super_client.wait_success(test_lb)
    return test_lb, config


def _create_config(super_client, admin_client, sim_context, listenerPort):
    # create config
    config = super_client. \
        create_loadBalancerConfig(name=random_str())
    config = super_client.wait_success(config)
    # create listener
    listener = _create_valid_listener(super_client,
                                      admin_client,
                                      sim_context,
                                      listenerPort)
    # add listener to config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    validate_add_listener(config, listener, super_client)

    return config


def _create_lb_w_host(super_client, admin_client,
                      sim_context, listenerPort, nsp):
    host = sim_context['host']

    # create lb
    lb, config = _create_valid_lb(super_client, admin_client,
                                  sim_context, listenerPort, nsp)

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
    return agent, lb, uri, instance, config


def _create_valid_listener(super_client, admin_client,
                           sim_context, sourcePort):
    listener = admin_client.create_loadBalancerListener(name=random_str(),
                                                        sourcePort=sourcePort,
                                                        sourceProtocol='http',
                                                        targetProtocol='http')
    listener = admin_client.wait_success(listener)
    return listener


def _wait_until_listener_map_active(listener, config,
                                    super_client, timeout=30):
    # need this function wait_success doesn't work for map object
    start = time.time()
    lb_config_maps = super_client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=listener.id,
                                           loadBalancerConfigId=config.id)
    listener_map = lb_config_maps[0]
    while listener_map.state != 'active':
        time.sleep(.5)
        listener_map = super_client.reload(listener_map)
        if time.time() - start > timeout:
            assert 'Timeout waiting for hostmap to be active.'

    return listener_map


def _resource_is_active(resource):
    return resource.state == 'active'


def _resource_is_removed(resource):
    return resource.state == 'removed'


def validate_add_listener(config, listener, super_client):
    lb_config_maps = super_client. \
        list_loadBalancerConfigListenerMap(loadBalancerListenerId=listener.id,
                                           loadBalancerConfigId=config.id)
    assert len(lb_config_maps) == 1
    config_map = lb_config_maps[0]
    wait_for_condition(
        super_client, config_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


def validate_add_target(admin_client, container1, lb, super_client):
    target_maps = admin_client. \
        list_loadBalancerTarget(loadBalancerId=lb.id,
                                instanceId=container1.id)
    assert len(target_maps) == 1
    target_map = target_maps[0]
    wait_for_condition(
        super_client, target_map, _resource_is_active,
        lambda x: 'State is: ' + x.state)


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
