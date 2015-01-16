from cattle import ApiError
from common_fixtures import *  # NOQA


def test_agent_unique(internal_test_client):
    agents = internal_test_client.list_agent(uri='sim://unique')

    if len(agents) == 0:
        agent = internal_test_client.create_agent(uri='sim://unique')
        agent = internal_test_client.wait_success(agent)
        assert agent.state == 'active'
        agent.deactivate()

    try:
        internal_test_client.create_agent(uri='sim://unique')
        assert False
    except ApiError, e:
        assert e.error.code == 'NotUnique'
        pass


def test_pagination(admin_client, sim_context):
    name = random_str()
    containers = []
    for i in range(4):
        c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                          name=name)
        containers.append(c)

    for c in containers:
        wait_success(admin_client, c)

    r = admin_client.list_container(name=name)

    assert len(r) == 4
    try:
        assert r.pagination.next is None
    except AttributeError:
        pass

    collected = {}
    r = admin_client.list_container(name=name, limit=2)
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


def test_pagination_include(internal_test_client, sim_context):
    name = random_str()
    container_ids = []
    containers = []
    host = sim_context['host']
    for i in range(5):
        c = internal_test_client.create_container(
            imageUuid=sim_context['imageUuid'],
            name=name, requestedHostId=host.id)
        containers.append(c)
        container_ids.append(c.id)

    for c in containers:
        wait_success(internal_test_client, c)

    assert len(containers[0].instanceHostMaps()) == 1
    assert host.id == containers[0].instanceHostMaps()[0].host().id
    r = internal_test_client.list_container(name=name)

    assert len(r) == 5
    for c in r:
        assert len(c.instanceHostMaps()) == 1
        assert c.instanceHostMaps()[0].hostId == host.id

    collected = {}
    r = internal_test_client.list_container(name=name,
                                            include='instanceHostMaps',
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
        maps.extend(internal_test_client.list_instanceHostMap(hostId=host.id,
                                                              instanceId=id))

    assert len(maps) == 5

    maps_from_include = []
    r = internal_test_client.list_host(include='instanceHostMaps', limit=2)

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


def test_include_left_join(admin_client, sim_context):
    image_uuid = sim_context['imageUuid']
    container = admin_client.create_container(imageUuid=image_uuid,
                                              startOnCreate=False)
    container = wait_success(admin_client, container)

    c = admin_client.by_id('container', container.id,
                           include='instanceHostMaps')

    assert container.id == c.id


def test_include(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    container = internal_test_client.create_container(imageUuid=image_uuid,
                                                      name='include_test')
    container = wait_success(internal_test_client, container)
    container = internal_test_client.reload(container)
    for link_name in ['instanceHostMaps', 'instancehostmaps']:
        found = False
        for c in internal_test_client.list_container(
                name_like='include_test%'):
            if c.id == container.id:
                found = True
                assert len(c.instanceHostMaps()) == 1
                assert callable(c.instanceHostMaps)

        assert found

        found = False
        for c in internal_test_client.list_container(
                include=link_name, name_like='include_test%'):
            if c.id == container.id:
                found = True
                assert len(c.instanceHostMaps) == 1

        assert found

        c = internal_test_client.by_id('container', container.id)
        assert callable(c.instanceHostMaps)
        c = internal_test_client.by_id('container', container.id,
                                       include=link_name)
        assert len(c.instanceHostMaps) == 1


def test_limit(admin_client, sim_context):
    result = admin_client.list_container()
    assert result.pagination.limit == 100

    result = admin_client.list_container(limit=105)
    assert result.pagination.limit == 105

    result = admin_client.list_container(limit=1005)
    assert result.pagination.limit == 1000


def test_schema_boolean_default(admin_client):
    con_schema = admin_client.schema.types['container']

    assert isinstance(con_schema.resourceFields.startOnCreate.default, bool)


def test_schema_self_link(admin_client):
    con_schema = admin_client.schema.types['container']

    assert con_schema.links.self is not None
    assert con_schema.links.self.startswith("http")


def test_child_map_include(admin_client, sim_context):
    image_uuid = sim_context['imageUuid']
    container = admin_client.create_container(imageUuid=image_uuid)
    container = wait_success(admin_client, container)

    cs = admin_client.list_container(uuid=container.uuid, include='hosts')

    assert cs[0].hosts[0].uuid is not None
    assert len(cs[0].hosts) == 1

    hs = admin_client.list_host(uuid=cs[0].hosts[0].uuid,
                                include='instances')

    found = False
    for i in hs[0].instances:
        if i.uuid == cs[0].uuid:
            found = True

    assert found


def test_child_map(admin_client, sim_context):
    image_uuid = sim_context['imageUuid']
    container = admin_client.create_container(imageUuid=image_uuid)
    container = wait_success(admin_client, container)

    hosts = container.hosts()
    assert len(hosts) == 1
    assert hosts[0].type == 'host'


def test_fields_on_include(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=sim_context['host'].id)
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    host = admin_client.by_id_host(sim_context['host'].id,
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


def test_state_enum(admin_client):
    container_schema = admin_client.schema.types['container']
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


def test_actions_based_on_state(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=sim_context['host'].id)
    c = admin_client.wait_success(c)
    assert c.state == 'running'
    assert set(c.actions.keys()) == set(['migrate', 'restart', 'stop',
                                         'update', 'execute'])


def test_include_user_not_auth_map(client, sim_context):
    client.list_host(include='instances')


def test_map_user_not_auth_map(client, sim_context):
    c = client.create_container(imageUuid=sim_context['imageUuid'],
                                requestedHostId=sim_context['host'].id)
    c = client.wait_success(c)

    assert c.state == 'running'
    assert len(c.hosts()) == 1
