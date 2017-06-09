from common_fixtures import *  # NOQA
import base64
import json


def test_service_proxy(client, context):
    env = client.create_stack(name=random_str())
    env = client.wait_success(env)
    assert env.state == 'active'

    service = create_and_activate(client, 'service', name=random_str(),
                                  launchConfig={
                                      'imageUuid': 'docker:nginx'},
                                  stackId=env.id)
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


def test_container_proxy_labels(context):
    labels = {
        'io.rancher.websocket.proxy.port': 1234,
        'io.rancher.websocket.proxy.scheme': 'https',
    }
    c = context.create_container(name=random_str(), labels=labels)
    access = c.proxy()
    assert access is not None
    assert access.url is not None
    assert access.token is not None

    s = access.token.split('.')[1]
    s += '=' * (-len(s) % 4)
    t = base64.decodestring(s)
    o = json.loads(t)

    assert o['proxy']['scheme'] == 'https'
    assert o['proxy']['address'] == c.primaryIpAddress + ':1234'
