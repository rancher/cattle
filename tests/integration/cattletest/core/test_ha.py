from common_fixtures import *  # NOQA


def _process_names(processes):
    return set([x.processName for x in processes])


def test_container_ha_default(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      data={'simForgetImmediately': True})
    c = admin_client.wait_success(c)
    ping = one(admin_client.list_task, name='agent.ping')
    ping.execute()

    def callback():
        processes = process_instances(admin_client, c, type='instance')
        if len(processes) < 3:
            return None
        return processes

    processes = wait_for(callback)

    c = admin_client.wait_success(c)
    assert c.state == 'stopped'

    assert _process_names(processes) == set(['instance.create',
                                             'instance.restart',
                                             'instance.stop'])


def test_container_ha_stop(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      instanceTriggeredStop='stop',
                                      data={'simForgetImmediately': True})
    c = admin_client.wait_success(c)
    ping = one(admin_client.list_task, name='agent.ping')
    ping.execute()

    def callback():
        processes = process_instances(admin_client, c, type='instance')
        if len(processes) < 3:
            return None
        return processes

    processes = wait_for(callback)

    c = admin_client.wait_success(c)
    assert c.state == 'stopped'

    assert _process_names(processes) == set(['instance.create',
                                             'instance.restart',
                                             'instance.stop'])


def test_container_ha_restart(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      instanceTriggeredStop='restart',
                                      data={'simForgetImmediately': True})
    c = admin_client.wait_success(c)
    ping = one(admin_client.list_task, name='agent.ping')
    ping.execute()

    def callback():
        processes = process_instances(admin_client, c, type='instance')
        if len(processes) < 4:
            return None
        return processes

    processes = wait_for(callback)

    c = admin_client.wait_success(c)
    assert c.state == 'running'

    assert _process_names(processes) == set(['instance.create',
                                             'instance.restart',
                                             'instance.stop',
                                             'instance.start'])


def test_container_ha_remove(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      instanceTriggeredStop='remove',
                                      data={'simForgetImmediately': True})
    c = admin_client.wait_success(c)
    ping = one(admin_client.list_task, name='agent.ping')
    ping.execute()

    def callback():
        processes = process_instances(admin_client, c, type='instance')
        if len(processes) < 4:
            return None
        return processes

    processes = wait_for(callback)

    c = admin_client.wait_success(c)
    assert c.state == 'removed'

    assert _process_names(processes) == set(['instance.create',
                                             'instance.restart',
                                             'instance.stop',
                                             'instance.remove'])
