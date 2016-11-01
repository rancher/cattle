from common_fixtures import *  # NOQA
import logging


def _process_names(processes):
    return set([x.processName for x in processes])


def test_container_ha_default(super_client, new_context):
    client = new_context.client
    c = new_context.\
        create_container_no_success(name='simForgetImmediately',
                                    imageUuid=new_context.image_uuid)

    def do_ping():
        ping = one(super_client.list_task, name='agent.ping')
        ping.execute()

    def callback():
        processes = process_instances(super_client, c, type='instance')
        if 'instance.stop' not in _process_names(processes):
            do_ping()
            return None
        return processes

    processes = wait_for(callback,
                         fail_handler=lambda: proc_err(c, super_client))

    c = wait_for_condition(client, c,
                           lambda x: x.state == 'removed',
                           lambda x: 'State is: ' + x.state)

    # TODO Remove this debugging block once we've seen this test not fail for
    # a few weeks. So, anytime after 6/4/2015.
    if c.state != 'stopped':
        logging.warn('test_container_ha_default debugging')
        for p in processes:
            logging.warn('ProcessInstance: %s' % p)
            for pe in process_executions(super_client, p.id):
                logging.warn('ProcessExecution: %s' % pe)

    assert _process_names(processes) == {'instance.create', 'instance.stop',
                                         'instance.remove'}


def test_container_discovery(super_client, new_context):
    client = new_context.client
    container_id = random_str()
    new_context.create_container_no_success(
        name='simCreateAnother_' + container_id,
        imageUuid=new_context.image_uuid)

    def do_ping():
        ping = one(super_client.list_task, name='agent.ping')
        ping.execute()

    def callback():
        cons = client.list_container(externalId=container_id)
        if len(cons) == 0:
            do_ping()
            return None
        return cons

    containers = wait_for(callback)
    assert len(containers) == 1
    container = containers[0]

    def running_callback():
        c = client.by_id_container(container.id)
        if c.state == 'running':
            return c

    wait_for(running_callback)


def test_container_ha_remove(super_client, new_context):
    c = new_context.super_create_container(instanceTriggeredStop='remove',
                                           imageUuid=new_context.image_uuid,
                                           data={'simForgetImmediately': True})
    c = super_client.wait_success(c)

    def do_ping():
        ping = one(super_client.list_task, name='agent.ping')
        ping.execute()

    def callback():
        processes = process_instances(super_client, c, type='instance')
        if 'instance.remove' not in _process_names(processes):
            do_ping()
            return None
        return processes

    processes = wait_for(callback,
                         fail_handler=lambda: proc_err(c, super_client))

    wait_for_condition(super_client, c,
                       lambda x: x.removed is not None,
                       lambda x: 'State is: ' + x.state)

    assert _process_names(processes) == {'instance.create',
                                         'instance.stop',
                                         'instance.remove'}


def process_executions(cli, id=None):
    return cli.list_process_execution(processInstanceId=id)


def proc_err(c, super_client):
    processes = process_instances(super_client, c, type='instance')
    c = super_client.reload(c)
    return 'Instance: %s\nProcesses: %s' % (c, processes.data)
