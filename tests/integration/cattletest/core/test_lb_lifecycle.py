from common_fixtures import *  # NOQA


@pytest.fixture(scope='session')
def docker_context(super_client):
    for host in super_client.list_host(state='active', remove_null=True,
                                       kind='docker'):
        return kind_context(super_client, 'docker', external_pool=True,
                            agent=host.agent())

    raise Exception('Failed to find docker host, please register one')


def test_add_lb_w_host_and_target(super_client, admin_client, docker_context):

    # add host
    agent, lb, uri, instance = _create_lb_w_host(super_client,
                                                 admin_client,
                                                 docker_context)
    # add target to a load balancer
    lb = lb.addtarget(instanceId=instance.id)
    admin_client.wait_success(lb)

    # TODO - test config items once the code is in


def test_add_removed_target_again(super_client, admin_client, docker_context):

    # add host
    agent, lb, uri, instance = _create_lb_w_host(super_client,
                                                 admin_client, docker_context)

    # add target to a load balancer
    lb = lb.addtarget(instanceId=instance.id)
    lb = admin_client.wait_success(lb)

    # remove the target
    lb = lb.removetarget(instanceId=instance.id)
    lb = admin_client.wait_success(lb)

    # add the target - should be allowed
    lb = lb.addtarget(instanceId=instance.id)
    lb = admin_client.wait_success(lb)

    # TODO - test config items once the code is in


def _create_valid_lb(super_client, admin_client, docker_context):
    config = _create_config(super_client, admin_client, docker_context)
    default_lb_config = super_client. \
        create_loadBalancerConfig(name=random_str())
    super_client.wait_success(default_lb_config)

    test_lb = super_client. \
        create_loadBalancer(name=random_str(),
                            loadBalancerConfigId=config.id)
    test_lb = super_client.wait_success(test_lb)
    return test_lb


def _create_config(super_client, admin_client, docker_context):
    # create config
    config = super_client. \
        create_loadBalancerConfig(name=random_str())
    config = super_client.wait_success(config)
    # create listener
    listener = _create_valid_listener(super_client,
                                      admin_client, docker_context)
    listener = super_client.wait_success(listener)
    # add listener to config
    config = config.addlistener(loadBalancerListenerId=listener.id)
    config = admin_client.wait_success(config)

    return config


def _create_lb_w_host(super_client, admin_client, docker_context):
    host = docker_context['host']

    # create lb
    lb = _create_valid_lb(super_client, admin_client, docker_context)

    # add host to lb
    lb.addhost(hostId=host.id)
    admin_client.wait_success(lb)

    # verify that the agent got created
    uri = 'delegate:///?lbId={}&hostId={}'. \
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


def _create_valid_listener(super_client, admin_client, docker_context):
    listener = admin_client.create_loadBalancerListener(name=random_str(),
                                                        sourcePort='8080',
                                                        targetPort='80',
                                                        sourceProtocol='http',
                                                        targetProtocol='tcp')
    listener = admin_client.wait_success(listener)
    return listener
