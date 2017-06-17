import requests
from cattle import ApiError

from common_fixtures import *  # NOQA


def test_agent_unique(super_client):
    agents = super_client.list_agent(uri='sim://unique')

    if len(agents) == 0:
        agent = super_client.create_agent(uri='sim://unique')
        agent = super_client.wait_success(agent)
        assert agent.state == 'active'
        agent.deactivate()

    try:
        super_client.create_agent(uri='sim://unique')
        assert False
    except ApiError, e:
        assert e.error.code == 'NotUnique'
        pass


def test_list_sort(new_context):
    client = new_context.client
    name = random_str()
    containers = []
    for i in range(2):
        c = new_context.create_container_no_success(name=name + random_str(),
                                                    startOnCreate=False,
                                                    description='test1')
        containers.append(c)

    r = client.list_container(description='test1')
    for i in range(len(r)):
        assert containers[i].id == r[i].id

    r = client.list_container(description='test1', sort='created',
                              order='desc')
    containers.reverse()
    for i in range(len(r)):
        assert containers[i].id == r[i].id


def test_pagination(context):
    client = context.client
    name = random_str()
    containers = []
    for i in range(4):
        c = client.create_container(imageUuid=context.image_uuid,
                                    name=name + random_str(),
                                    description='test2')
        containers.append(c)

    for c in containers:
        client.wait_success(c)

    r = client.list_container(description='test2')

    assert len(r) == 4
    try:
        assert r.pagination.next is None
    except AttributeError:
        pass

    collected = {}
    r = client.list_container(description='test2', limit=2)
    assert len(r) == 2
    assert r.pagination.next is not None

    for i in r:
        collected[i.id] = True

    r = r.next()

    assert len(r) == 2
    try:
        assert r.pagination.next is None
    except AttributeError:
        pass

    for i in r:
        collected[i.id] = True

    assert len(collected) == 4


def test_limit(super_client):
    result = super_client.list_container()
    assert result.pagination.limit == 100

    result = super_client.list_container(limit=105)
    assert result.pagination.limit == 105

    result = super_client.list_container(limit=10005)
    assert result.pagination.limit == 3000


def test_schema_boolean_default(client):
    con_schema = client.schema.types['container']
    assert isinstance(con_schema.resourceFields.startOnCreate.default, bool)


def test_schema_self_link(client):
    con_schema = client.schema.types['container']
    assert con_schema.links.self is not None
    assert con_schema.links.self.startswith("http")


def test_child_map(super_client, context):
    container = context.create_container()

    host = super_client.reload(container).host()
    assert host.type == 'host'


def test_state_enum(super_client):
    container_schema = super_client.schema.types['container']
    states = set([
        'creating',
        'removed',
        'removing',
        'requested',
        'restarting',
        'running',
        'starting',
        'stopped',
        'stopping',
        'updating-running',
        'updating-stopped',
        'error',
        'erroring'
    ])

    assert container_schema.resourceFields['state'].type == 'enum'
    assert states == set(container_schema.resourceFields['state'].options)


def test_actions_based_on_state(context):
    c = context.create_container()
    assert set(c.actions.keys()) == set(['restart', 'stop', 'update',
                                         'execute', 'logs', 'proxy',
                                         'converttoservice', 'upgrade'])


def test_map_user_not_auth_map(context):
    c = context.create_container()
    assert c.hostId is not None


def test_query_length(admin_user_client):
    big = 'a' * 8192
    admin_user_client.list_account(name=big)

    bigger = 'a' * (16384 - 512)
    admin_user_client.list_account(name=bigger)


def test_x_bad_forwarded(cattle_url):
    resp = requests.get(cattle_url, headers={'x-forwarded-for': '1.1.1.1'})
    assert resp.headers['x-api-client-ip'] == '1.1.1.1'

    resp = requests.get(cattle_url, headers={
        'x-forwarded-for': '1.1.1.1:1234'
    })
    assert resp.headers['x-api-client-ip'] == '1.1.1.1'
