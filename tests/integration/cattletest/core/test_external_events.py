from common_fixtures import *  # NOQA
from cattle import ApiError

SERVICE_KIND = 'kubernetesService'


def from_context(context):
    return context.client, context.agent_client, context.host


def test_bad_agent(super_client, new_context):
    _, account, agent_client = register_simulated_host(new_context,
                                                       return_agent=True)

    def post():
        external_id = random_str()
        agent_client.create_external_storage_pool_event(
            externalId=external_id,
            eventType="storagepool.create",
            hostUuids=[],
            storagePool={
                'name': 'name-%s' % external_id,
                'externalId': external_id,
            })

    # Test it works
    post()

    # Test it fails with two agents
    super_client.wait_success(super_client.create_agent(
        uri='test://' + random_str(),
        accountId=account.id))
    with pytest.raises(ApiError) as e:
        post()
    assert e.value.error.code == 'MissingRequired'

    # Test it fails with no agents
    for agent in super_client.list_agent(accountId=account.id):
        super_client.wait_success(agent.deactivate())
    with pytest.raises(ApiError) as e:
        post()
    assert e.value.error.code == 'CantVerifyAgent'


def test_external_host_event_miss(new_context):
    new_context.create_container()

    client = new_context.client
    host = new_context.host
    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate',
                                              deleteHost=True)
    event = client.wait_success(event)
    host = client.reload(host)

    assert event.state == 'created'
    assert host.state == 'active'


def test_external_host_event_wrong_event(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.notevacuate',
                                              deleteHost=True)
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'active'
    assert c.state == 'running'


def test_external_host_event_hit(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.wait_success(new_context.host)
    host = client.update(host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate',
                                              deleteHost=True)
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'purged'
    assert c.state == 'removed'


def test_external_host_event_no_delete(new_context):
    c = new_context.create_container()

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostLabel='foo=bar',
                                              eventType='host.evacuate')
    assert event.state == 'creating'

    event = client.wait_success(event)
    host = client.reload(host)
    c = client.wait_success(c)

    assert event.state == 'created'
    assert host.state == 'inactive'


def test_external_host_event_by_id(new_context):
    c = new_context.create_container()
    new_host = register_simulated_host(new_context)

    client = new_context.client
    host = client.update(new_context.host, labels={
        'foo': 'bar'
    })
    host = client.wait_success(host)
    assert host.labels == {'foo': 'bar'}

    event = client.create_external_host_event(hostId=host.id,
                                              eventType='host.evacuate')
    assert event.state == 'creating'

    event = client.wait_success(event)
    new_host = client.reload(new_host)
    c = client.wait_success(c)
    host = client.reload(host)

    assert event.state == 'created'
    assert host.state == 'inactive'
    assert new_host.state == 'active'


def test_external_dns_event(super_client, new_context):
    client, agent_client, host = from_context(new_context)

    stack = client.create_stack(name=random_str())
    stack = client.wait_success(stack)
    image_uuid = new_context.image_uuid
    launch_config = {"imageUuid": image_uuid}

    svc1 = client.create_service(name=random_str(),
                                 stackId=stack.id,
                                 launchConfig=launch_config)
    svc1 = client.wait_success(svc1)

    domain_name1 = "foo.com"
    create_dns_event(client, agent_client, super_client,
                     new_context, svc1.name,
                     stack.name, domain_name1)

    # wait for dns name to be updated
    svc1 = client.reload(svc1)
    assert svc1.fqdn == domain_name1


def create_dns_event(client, agent_client, super_client,
                     context, svc_name1,
                     stack_name, domain_name):
    external_id = random_str()
    event_type = "externalDnsEvent"
    dns_event = {
        'externalId': external_id,
        'eventType': event_type,
        "stackName": stack_name,
        "serviceName": svc_name1,
        "fqdn": domain_name
    }

    event = agent_client.create_external_dns_event(dns_event)
    assert event.externalId == external_id
    assert event.eventType == event_type
    event = wait_for(lambda: event_wait(client, event))
    assert event.accountId == context.project.id
    assert event.reportedAccountId == context.agent.id

    return event


def test_external_service_event_create(client, context, super_client):
    agent_client = context.agent_client

    env_external_id = random_str()
    environment = {"name": "foo", "externalId": env_external_id}

    svc_external_id = random_str()
    svc_name = 'svc-name-%s' % svc_external_id
    selector = 'foo=bar1'
    template = {'foo': 'bar'}
    svc_data = {
        'selectorContainer': selector,
        'kind': SERVICE_KIND,
        'name': svc_name,
        'externalId': svc_external_id,
        'template': template,
    }
    event = agent_client.create_external_service_event(
        eventType='service.create',
        environment=environment,
        externalId=svc_external_id,
        service=svc_data,
    )

    event = wait_for(lambda: event_wait(client, event))
    assert event is not None

    svc = wait_for(lambda: service_wait(client, svc_external_id))

    assert svc.externalId == svc_external_id
    assert svc.name == svc_name
    assert svc.kind == SERVICE_KIND
    assert svc.selectorContainer == selector
    assert svc.stackId is not None
    assert svc.template == template

    envs = client.list_stack(externalId=env_external_id)
    assert len(envs) == 1
    assert envs[0].id == svc.stackId

    wait_for_condition(client, svc,
                       lambda x: x.state == 'active',
                       lambda x: 'State is: ' + x.state)

    # Update
    new_selector = 'newselector=foo'
    svc_data = {
        'selectorContainer': new_selector,
        'kind': SERVICE_KIND,
        'template': {'foo': 'bar'},
    }
    agent_client.create_external_service_event(
        eventType='service.update',
        environment=environment,
        externalId=svc_external_id,
        service=svc_data,
    )

    wait_for_condition(client, svc,
                       lambda x: x.selectorContainer == new_selector,
                       lambda x: 'Selector is: ' + x.selectorContainer)

    # Delete
    agent_client.create_external_service_event(
        name=svc_name,
        eventType='service.remove',
        externalId=svc_external_id,
        service={'kind': SERVICE_KIND},
    )

    wait_for_condition(client, svc,
                       lambda x: x.state == 'removed',
                       lambda x: 'State is: ' + x.state)


def test_external_stack_event_create(client, context, super_client):
    agent_client = context.agent_client

    env_external_id = random_str()
    stack = {"name": env_external_id, "externalId": env_external_id,
             "kind": "environment"}

    env = client.create_stack(stack)
    env = client.wait_success(env)

    service = {
        'kind': SERVICE_KIND,
    }

    event = agent_client.create_external_service_event(
        eventType='stack.remove',
        environment=stack,
        externalId=env_external_id,
        service=service,
    )

    event = wait_for(lambda: event_wait(client, event))
    assert event is not None

    wait_for(lambda: len(client.list_stack(externalId=env_external_id)) == 0)


def service_wait(client, external_id):
    services = client.list_kubernetes_service(externalId=external_id)
    if len(services) and services[0].state == 'active':
        return services[0]


def event_wait(client, event):
    created = client.by_id('externalEvent', event.id)
    if created is not None and created.state == 'created':
        return created
