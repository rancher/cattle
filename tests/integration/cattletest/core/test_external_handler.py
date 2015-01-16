from common_fixtures import *  # NOQA

TEST_HANDLER_PREFIX = 'test-handler-'


@pytest.fixture(scope='module', autouse=True)
def tear_down(request, internal_test_client):
    request.addfinalizer(lambda: _disable_test_handlers(internal_test_client))


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


def _disable_test_handlers(internal_test_client):
    name = TEST_HANDLER_PREFIX + '%'
    for h in internal_test_client.list_external_handler(state='active',
                                                        name_like=name):
        wait_success(internal_test_client, h.deactivate())


def test_external_handler(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    h = internal_test_client.create_external_handler(
        name=name, processNames=['instance.start'])

    assert h.state == 'registering'
    assert h.get('processNames') is None
    assert h.data.fields.processNames == ['instance.start']

    h = wait_success(internal_test_client, h)

    assert h.state == 'active'
    assert h.data.fields.processNames is None

    maps = h.externalHandlerExternalHandlerProcessMaps()
    assert len(maps) == 1
    assert maps[0].state == 'active'

    process = maps[0].externalHandlerProcess()
    assert process.state == 'active'
    assert process.name == 'instance.start'

    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None


def test_defaults(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    h = internal_test_client.create_external_handler(
        name=name,
        processNames=['instance.start'])
    h = wait_success(internal_test_client, h)
    assert h.state == 'active'

    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None

    assert ep.properties.retry is None
    assert ep.properties.timeoutMillis is None
    assert ep.properties.name == name
    assert ep.properties.priority == '1000'
    assert ep.properties.eventName == 'instance.start;handler={}'.format(name)


def test_properties(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    h = internal_test_client.create_external_handler(
        name=name, processNames=['instance.start'], timeoutMillis=2000,
        retries=4, priority=1234)
    h = wait_success(internal_test_client, h)
    assert h.state == 'active'

    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None

    assert ep.properties.retry == '4'
    assert ep.properties.timeoutMillis == '2000'
    assert ep.properties.priority == '1234'
    assert ep.properties.name == name
    assert ep.properties.eventName == 'instance.start;handler={}'.format(name)


def test_pre_handler(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    process_names = ['pre.instance.start']
    h = internal_test_client.create_external_handler(
        name=name, processNames=process_names,
        timeoutMillis=2000, retries=4, priority=1234)
    h = wait_success(internal_test_client, h)
    assert h.state == 'active'

    ep = _get_extension(internal_test_client,
                        'process.instance.start.pre.listeners',
                        name)
    assert ep is not None
    assert ep.properties.eventName == \
        'pre.instance.start;handler={}'.format(name)


def test_post_handler(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    event_names = ['post.instance.start']
    h = internal_test_client.create_external_handler(name=name,
                                                     processNames=event_names,
                                                     timeoutMillis=2000,
                                                     retries=4,
                                                     priority=1234)
    h = wait_success(internal_test_client, h)
    assert h.state == 'active'

    ep = _get_extension(internal_test_client,
                        'process.instance.start.post.listeners',
                        name)
    assert ep is not None
    assert ep.properties.eventName == \
        'post.instance.start;handler={}'.format(name)


def test_enabled_disable(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    h = internal_test_client.create_external_handler(
        name=name,
        processNames=['instance.start'])
    h = wait_success(internal_test_client, h)

    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None

    h = wait_success(internal_test_client, h.deactivate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is None

    h = wait_success(internal_test_client, h.activate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None

    wait_success(internal_test_client,
                 h.externalHandlerProcesses()[0].deactivate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is None

    wait_success(internal_test_client,
                 h.externalHandlerProcesses()[0].activate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None

    wait_success(internal_test_client,
                 h.externalHandlerExternalHandlerProcessMaps()[0].deactivate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is None

    wait_success(internal_test_client,
                 h.externalHandlerExternalHandlerProcessMaps()[0].activate())
    ep = _get_extension(internal_test_client,
                        'process.instance.start.handlers',
                        name)
    assert ep is not None


def test_event_name_comma(internal_test_client):
    name = '{}-{}'.format(TEST_HANDLER_PREFIX, random_str())
    event_names = ['pre.instance.start,instance.start'],
    h = internal_test_client.create_external_handler(name=name,
                                                     processNames=event_names)
    h = wait_success(internal_test_client, h)

    processes = [x.name for x in h.externalHandlerProcesses()]

    assert len(processes) == 2
    assert 'pre.instance.start' in processes
    assert 'instance.start' in processes
