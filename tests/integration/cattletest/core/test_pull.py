from common_fixtures import *  # NOQA


def test_pull(new_context):
    client = new_context.client
    host1 = new_context.host
    host2 = register_simulated_host(new_context.client)

    status = {
        host1.name: 'Done',
        host2.name: 'Done',
    }

    task = client.create_pull_task(image='fake')

    assert task.state == 'registering'
    task = client.wait_success(task)

    assert task.state == 'active'
    assert task.status == status
