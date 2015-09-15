from common_fixtures import *  # NOQA


def test_pull(client, context):
    task = client.create_pull_task(image='fake')

    assert task.state == 'registering'
    task = client.wait_success(task)

    assert task.state == 'active'
