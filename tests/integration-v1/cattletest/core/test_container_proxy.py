from common import *  # NOQA


def test_service_proxy(client, context):
    env = client.create_environment(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'

    service = create_and_activate(client, 'service', name=random_str(),
                                  launchConfig={
                                      'imageUuid': 'docker:nginx'},
                                  environmentId=env.id)
    assert service.state == 'active'

    access = client.create_service_proxy(service=service.name)
    assert access is not None
    assert access.url is not None
    assert access.token is not None


def test_container_proxy(context):
    c = context.create_container(name=random_str())
    access = c.proxy()
    assert access is not None
    assert access.url is not None
    assert access.token is not None
