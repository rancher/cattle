from common_fixtures import *  # NOQA


def _process_names(processes):
    return set([x.processName for x in processes])


def test_container_ha_default(client, super_client, user_sim_context):
    c = client.create_container(imageUuid=user_sim_context['imageUuid'],
                                requestedHostId=user_sim_context['host'].id,
                                name='simForgetImmediately')
    c = client.wait_success(c)

    def do_ping():
        ping = one(super_client.list_task, name='agent.ping')
        ping.execute()

    def callback():
        processes = process_instances(super_client, c, type='instance')
        if 'instance.stop' not in _process_names(processes):
            do_ping()
            return None
        return processes

    processes = wait_for(callback)

    c = super_client.wait_success(c)

    for p in processes:
        print p
        for pe in process_executions(super_client, p.id):
            print pe
        print '\n\n'

    assert c.state == 'stopped'

    assert _process_names(processes) == set(['instance.create',
                                             'instance.stop'])


def test_container_ha_stop(super_client, sim_context):
    pass


def process_executions(cli, id=None):
    return cli.list_process_execution(processInstanceId=id)
