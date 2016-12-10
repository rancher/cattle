from common_fixtures import *  # NOQA


def test_generic_object(client):
    uuid = random_str()
    name = "Scale out service X by 1"
    kind = 'webhookToken'
    generic_object = client.create_generic_object(name=name,
                                                  key=uuid,
                                                  kind=kind)

    client.wait_success(generic_object)

    webhooks = client.list_generic_object(kind=kind, key=uuid)
    assert len(webhooks.data) > 0

    client.wait_success(client.delete(generic_object))
    webhooks = client.list_generic_object(kind=kind, key=uuid)
    assert len(webhooks.data) == 0
