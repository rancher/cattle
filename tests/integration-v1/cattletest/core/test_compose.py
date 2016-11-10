from common import *  # NOQA


SERVICE = 'com.docker.compose.service'
PROJECT = 'com.docker.compose.project'
NUMBER = 'com.docker.compose.container-number'


def test_container_create_count(client, context):
    project, service, c = _create_service(client, context)

    assert c.labels['io.rancher.service.deployment.unit'] is not None
    assert c.labels['io.rancher.service.launch.config'] == \
        'io.rancher.service.primary.launch.config'
    assert c.labels['io.rancher.stack_service.name'] == project + '/' + service
    assert c.labels['io.rancher.stack.name'] == project

    s = find_one(c.services)
    s = client.wait_success(s)
    env = client.wait_success(s.environment())

    assert s.name == service
    assert s.type == 'composeService'
    assert s.kind == 'composeService'
    assert s.state == 'active'
    assert s.state == 'active'
    selector = 'com.docker.compose.project={}, ' \
               'com.docker.compose.service={}'.format(project, service)
    assert s.selectorContainer == selector
    assert env.name == project
    assert env.state == 'active'
    assert env.type == 'composeProject'
    assert env.kind == 'composeProject'

    assert set(env.actions.keys()) == set(['remove'])
    assert set(s.actions.keys()) == set(['remove'])


def _create_service(client, context, project=None, service=None):
    if project is None:
        project = 'p-' + random_str()
    if service is None:
        service = 's-' + random_str()
    c = context.create_container(name='{}_{}_1'.format(service, project),
                                 labels={
                                     SERVICE: service,
                                     PROJECT: project,
                                     NUMBER: '1',
                                     }, networkMode='none')

    assert c.state == 'running'
    return project, service, c


def test_container_remove(client, context):
    project, service, c = _create_service(client, context)

    s = find_one(c.services)
    map = find_one(s.serviceExposeMaps)
    s = client.wait_success(s)
    env = client.wait_success(s.environment())

    c = client.delete(c)
    c = client.wait_success(c)
    assert c.state == 'removed'

    wait_for(lambda: client.reload(map).state != 'active')
    map = client.wait_success(map)
    assert map.state == 'removed'

    s = client.wait_success(s)
    env = client.wait_success(env)

    assert s.state == 'removed'
    assert env.state == 'removed'


def test_container_two_remove(client, context):
    project, service, c = _create_service(client, context)
    project, service, c = _create_service(client, context, project, service)

    s = find_one(c.services)
    maps = s.serviceExposeMaps()
    s = client.wait_success(s)
    env = client.wait_success(s.environment())

    assert len(maps) == 2

    c = client.delete(c)
    c = client.wait_success(c)
    assert c.state == 'removed'

    wait_for(lambda: len([x for x in s.serviceExposeMaps()
                          if x.removed is None]) == 1)

    s = client.wait_success(s)
    env = client.wait_success(env)

    assert s.state == 'active'
    assert env.state == 'active'


def test_service_two_remove(client, context):
    project, service, c = _create_service(client, context)
    project, _, _ = _create_service(client, context, project)

    s = find_one(c.services)
    map = find_one(s.serviceExposeMaps)
    s = client.wait_success(s)
    env = client.wait_success(s.environment())
    assert len(env.services()) == 2

    assert s.state == 'active'

    s = client.delete(s)
    s = client.wait_success(s)
    assert s.state == 'removed'

    map = client.wait_success(map)
    assert map.state == 'removed'

    c = client.wait_success(c)
    assert c.state == 'removed'

    env = client.wait_success(env)
    assert env.state == 'active'


def test_service_remove(client, context):
    project, service, c = _create_service(client, context)

    s = find_one(c.services)
    map = find_one(s.serviceExposeMaps)
    s = client.wait_success(s)
    env = client.wait_success(s.environment())

    assert s.state == 'active'

    s = client.delete(s)
    s = client.wait_success(s)
    assert s.state == 'removed'

    map = client.wait_success(map)
    assert map.state == 'removed'

    c = client.wait_success(c)
    assert c.state == 'removed'

    env = client.wait_success(env)
    assert env.state == 'removed'


def test_env_remove(client, context):
    project, service, c = _create_service(client, context)

    s = find_one(c.services)
    map = find_one(s.serviceExposeMaps)
    s = client.wait_success(s)
    env = client.wait_success(s.environment())

    assert s.state == 'active'

    env = client.delete(env)
    env = client.wait_success(env)
    assert env.state == 'removed'

    s = client.wait_success(s)
    assert s.state == 'removed'

    map = client.wait_success(map)
    assert map.state == 'removed'

    c = client.wait_success(c)
    assert c.state == 'removed'


def test_compose_project_create_required(client, context):
    template = 'nginx:\n  image: nginx'
    assert_required_fields(client.create_compose_project, name=random_str(),
                           templates={'x': template})


def test_compose_project_create(client, context):
    name = random_str()
    template = 'nginx:' \
               '  image: nginx'
    project = client.create_compose_project(name=name,
                                            templates={'x': template})
    project = client.wait_success(project)
    assert project.name == name
    assert project.state == 'active'
    assert project.type == 'composeProject'
    assert project.kind == 'composeProject'
    assert project.templates == {'x': template}
