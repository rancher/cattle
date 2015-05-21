from common_fixtures import *  # NOQA

TEST_HANDLER_PREFIX = 'test-handler-'


@pytest.fixture(scope='module', autouse=True)
def tear_down(request, admin_user_client):
    request.addfinalizer(lambda: _disable_test_handlers(admin_user_client))


def _get_extension(admin_client, extension_point_name, impl_name,
                   format='Dynamic : {}'):
    for ep in admin_client.list_extension_point():
        if ep.name == extension_point_name:
            for impl in ep.implementations:
                try:
                    if impl.properties.name == impl_name:
                        return impl
                except AttributeError:
                    pass

                if impl.name == format.format(impl_name):
                    return impl

    return None


def _disable_test_handlers(client):
    name = TEST_HANDLER_PREFIX + '%'
    for h in client.list_external_handler(state='active',
                                                name_like=name):
        client.wait_success(h.deactivate())


def test_external_handler(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'instance.start', 'onError': 'instance.stop'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs)

    assert h.state == 'registering'
    assert h.get('processConfigs') is None
    assert h.data.fields.processConfigs == configs

    h = admin_user_client.wait_success(h)

    assert h.state == 'active'
    assert h.data.fields.processConfigs is None

    maps = h.externalHandlerExternalHandlerProcessMaps()
    assert len(maps) == 1
    assert maps[0].state == 'active'
    assert maps[0].onError == 'instance.stop'

    process = maps[0].externalHandlerProcess()
    assert process.state == 'active'
    assert process.name == 'instance.start'

    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None


def test_defaults(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs)
    h = admin_user_client.wait_success(h)
    assert h.state == 'active'

    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None

    assert ep.properties.retry is None
    assert ep.properties.timeoutMillis is None
    assert ep.properties.name == name
    assert ep.properties.priority == '1000'
    assert ep.properties.eventName == 'instance.start;handler={}'.format(name)


def test_properties(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs,
                                                  timeoutMillis=2000,
                                                  retries=4,
                                                  priority=1234)
    h = admin_user_client.wait_success(h)
    assert h.state == 'active'

    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None

    assert ep.properties.retry == '4'
    assert ep.properties.timeoutMillis == '2000'
    assert ep.properties.priority == '1234'
    assert ep.properties.name == name
    assert ep.properties.eventName == 'instance.start;handler={}'.format(name)


def test_pre_handler(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'pre.instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs,
                                                  timeoutMillis=2000,
                                                  retries=4,
                                                  priority=1234)
    h = admin_user_client.wait_success(h)
    assert h.state == 'active'

    ep = _get_extension(admin_user_client,
                        'process.instance.start.pre.listeners',
                        name)
    assert ep is not None
    assert ep.properties.eventName == \
        'pre.instance.start;handler={}'.format(name)


def test_post_handler(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'post.instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs,
                                                  timeoutMillis=2000,
                                                  retries=4,
                                                  priority=1234)
    h = admin_user_client.wait_success(h)
    assert h.state == 'active'

    ep = _get_extension(admin_user_client,
                        'process.instance.start.post.listeners',
                        name)
    assert ep is not None
    assert ep.properties.eventName == \
        'post.instance.start;handler={}'.format(name)


def test_enabled_disable(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs)
    h = admin_user_client.wait_success(h)

    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None

    h = admin_user_client.wait_success(h.deactivate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is None

    h = admin_user_client.wait_success(h.activate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None

    admin_user_client.wait_success(
        h.externalHandlerProcesses()[0].deactivate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is None

    admin_user_client.wait_success(h.externalHandlerProcesses()[0].activate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None

    admin_user_client.wait_success(
        h.externalHandlerExternalHandlerProcessMaps()[0].deactivate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is None

    admin_user_client.wait_success(
        h.externalHandlerExternalHandlerProcessMaps()[0].activate())
    ep = _get_extension(admin_user_client, 'process.instance.start.handlers',
                        name)
    assert ep is not None


def test_event_name_comma(admin_user_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    configs = [{'name': 'pre.instance.start,instance.start'}]
    h = admin_user_client.create_external_handler(name=name,
                                                  processConfigs=configs)
    h = admin_user_client.wait_success(h)

    processes = [x.name for x in h.externalHandlerProcesses()]

    assert len(processes) == 2
    assert 'pre.instance.start' in processes
    assert 'instance.start' in processes
