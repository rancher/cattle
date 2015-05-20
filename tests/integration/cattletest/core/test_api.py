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


def test_pagination(context):
    client = context.client
    name = random_str()
    containers = []
    for i in range(4):
        c = client.create_container(imageUuid=context.image_uuid, name=name)
        containers.append(c)

    for c in containers:
        client.wait_success(c)

    r = client.list_container(name=name)

    assert len(r) == 4
    try:
        assert r.pagination.next is None
    except AttributeError:
        pass

    collected = {}
    r = client.list_container(name=name, limit=2)
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


def test_pagination_include(super_client, client, context):
    host = context.host
    name = random_str()
    container_ids = []
    containers = []
    for i in range(5):
        c = client.create_container(imageUuid=context.image_uuid,
                                    name=name,
                                    requestedHostId=host.id)
        c = super_client.reload(c)
        containers.append(c)
        container_ids.append(c.id)

    for c in containers:
        client.wait_success(c)

    assert len(containers[0].instanceHostMaps()) == 1
    assert host.id == containers[0].instanceHostMaps()[0].host().id
    r = super_client.list_container(name=name)

    assert len(r) == 5
    for c in r:
        assert len(c.instanceHostMaps()) == 1
        assert c.instanceHostMaps()[0].hostId == host.id

    collected = {}
    r = super_client.list_container(name=name, include='instanceHostMaps',
                                    limit=2)
    assert len(r) == 2
    for c in r:
        collected[c.id] = True
        assert len(c.instanceHostMaps) == 1
        assert c.instanceHostMaps[0].hostId == host.id

    r = r.next()

    assert len(r) == 2
    for c in r:
        collected[c.id] = True
        assert len(c.instanceHostMaps) == 1
        assert c.instanceHostMaps[0].hostId == host.id

    r = r.next()

    assert len(r) == 1
    for c in r:
        collected[c.id] = True
        assert len(c.instanceHostMaps) == 1
        assert c.instanceHostMaps[0].hostId == host.id

    assert not r.pagination.partial

    maps = []
    for id in container_ids:
        maps.extend(super_client.list_instanceHostMap(hostId=host.id,
                                                      instanceId=id))

    assert len(maps) == 5

    maps_from_include = []
    r = super_client.list_host(include='instanceHostMaps', limit=2)

    while True:
        for h in r:
            if h.id == host.id:
                assert len(h.instanceHostMaps) <= 2
                for m in h.instanceHostMaps:
                    if m.instanceId in container_ids:
                        maps_from_include.append(m)

        try:
            r = r.next()
        except AttributeError:
            break

    assert len(maps) == len(maps_from_include)


def test_include_left_join(super_client, context):
    container = context.create_container_no_success(startOnCreate=False)
    container = context.wait_for_state(container, 'stopped')
    c = super_client.by_id('container', container.id,
                           include='instanceHostMaps')

    assert container.id == c.id


def test_include(super_client, context):
    container = context.create_container(name='include_test')
    container = super_client.reload(container)

    for link_name in ['instanceHostMaps', 'instancehostmaps']:
        found = False
        for c in super_client.list_container(name_like='include_test%'):
            if c.id == container.id:
                found = True
                assert len(c.instanceHostMaps()) == 1
                assert callable(c.instanceHostMaps)

        assert found

        found = False
        for c in super_client.list_container(include=link_name,
                                             name_like='include_test%'):
            if c.id == container.id:
                found = True
                assert len(c.instanceHostMaps) == 1

        assert found

        c = super_client.by_id('container', container.id)
        assert callable(c.instanceHostMaps)
        c = super_client.by_id('container', container.id, include=link_name)
        assert len(c.instanceHostMaps) == 1


def test_limit(super_client):
    result = super_client.list_container()
    assert result.pagination.limit == 100

    result = super_client.list_container(limit=105)
    assert result.pagination.limit == 105

    result = super_client.list_container(limit=1005)
    assert result.pagination.limit == 1000


def test_schema_boolean_default(client):
    con_schema = client.schema.types['container']
    assert isinstance(con_schema.resourceFields.startOnCreate.default, bool)


def test_schema_self_link(client):
    con_schema = client.schema.types['container']
    assert con_schema.links.self is not None
    assert con_schema.links.self.startswith("http")


def test_child_map_include(super_client, context):
    container = context.create_container()

    cs = super_client.list_container(uuid=container.uuid, include='hosts')

    assert cs[0].hosts[0].uuid is not None
    assert len(cs[0].hosts) == 1

    hs = super_client.list_host(uuid=cs[0].hosts[0].uuid,
                                include='instances')

    found = False
    for i in hs[0].instances:
        if i.uuid == cs[0].uuid:
            found = True

    assert found


def test_child_map(super_client, context):
    container = context.create_container()

    hosts = super_client.reload(container).hosts()
    assert len(hosts) == 1
    assert hosts[0].type == 'host'


def test_fields_on_include(super_client, context):
    c = context.create_container()
    host = super_client.by_id_host(context.host.id,
                                   include='instances')

    assert host is not None

    found = False
    for instance in host.instances:
        if instance.id == c.id:
            assert instance.transitioning == 'no'
            assert 'stop' in instance
            assert callable(instance.stop)
            assert len(instance.links) > 1
            found = True
            break

    assert found


def test_state_enum(super_client):
    container_schema = super_client.schema.types['container']
    states = set([
        'creating',
        'migrating',
        'purged',
        'purging',
        'removed',
        'removing',
        'requested',
        'restarting',
        'restoring',
        'running',
        'starting',
        'stopped',
        'stopping',
        'updating-running',
        'updating-stopped'
    ])

    assert container_schema.resourceFields['state'].type == 'enum'
    assert states == set(container_schema.resourceFields['state'].options)


def test_actions_based_on_state(context):
    c = context.create_container()
    assert set(c.actions.keys()) == set(['migrate', 'restart', 'stop',
                                         'update', 'execute', 'logs',
                                         'addlabel', 'removelabel',
                                         'setlabels'])


def test_include_user_not_auth_map(client):
    client.list_host(include='instances')


def test_map_user_not_auth_map(context):
    c = context.create_container()
    assert len(c.hosts()) == 1


def test_role_option(admin_user_client, client, random_str, context):
    c = admin_user_client.create_api_key(name=random_str,
                                         accountId=context.account.id)
    c = admin_user_client.wait_success(c)

    assert c.state == 'active'

    creds = admin_user_client.list_credential(name=random_str)
    assert len(creds) == 0

    creds = admin_user_client.list_credential(name=random_str,
                                              _role='superadmin')
    assert len(creds) == 1

    creds = client.list_credential(name=random_str, _role='superadmin')
    assert len(creds) == 0
