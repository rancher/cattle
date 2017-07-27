from common_fixtures import *  # NOQA


def test_generic_object(client):
    uuid = random_str()
    name = "Scale out service X by 1"
    token = random_str()
    url = cattle_url() + "?token=" + token
    resource_data = {
        "url": url,
        "driver": "scaleService",
        "config": {"serviceId": "1s1", "amount": 4}
    }
    generic_obj = client.create_generic_object(name=name,
                                               key=uuid,
                                               kind="webhookReceiver",
                                               resourceData=resource_data)
    generic_obj = client.wait_success(generic_obj)
    assert generic_obj.name == name
    assert generic_obj.resourceData == resource_data

    assert len(
        client.list_generic_object(kind="webhookReceiver", key=uuid)) == 1

    client.wait_success(client.delete(generic_obj))
    generic_objs = client.list_generic_object(key=uuid)
    assert len(generic_objs.data) == 0
