from common_fixtures import *  # NOQA


def test_create_k8s_container_no_k8s(context):
    c = context.create_container(labels={
        'io.kubernetes.pod.namespace': 'n',
        'io.kubernetes.pod.name': 'p',
        'io.kubernetes.container.name': 'POD',
    })
    c = context.client.wait_success(c)
    assert c.state == 'running'


def test_create_k8s_container_no_k8s_fail(new_context, super_client):
    client = new_context.client
    c = new_context.create_container(labels={
        'io.kubernetes.pod.namespace': 'n',
        'io.kubernetes.pod.name': 'p',
        'io.kubernetes.container.name': 'POD',
    }, startOnCreate=False)

    super_client.update(c.account(), orchestration='k8s')
    c = client.wait_transitioning(c.start())
    assert c.transitioning == 'error'
    assert c.transitioningMessage == 'Failed to find labels provider'
