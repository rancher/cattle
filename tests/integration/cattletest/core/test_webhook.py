from common_fixtures import *  # NOQA


def test_webhook(client):
    uuid = random_str()
    name = "Scale out service X by 1"
    token = random_str()
    url = cattle_url() + "?token=" + token
    user_input = {
        "name": "Scale out service X by 1",
        "driver": "scaleService"
    }
    webhook = client.create_webhook(name=name,
                                    key=uuid,
                                    url=url,
                                    input=user_input)

    client.wait_success(webhook)

    webhooks = client.list_webhook(key=uuid)
    assert len(webhooks.data) > 0

    client.wait_success(client.delete(webhook))
    webhooks = client.list_webhook(key=uuid)
    assert len(webhooks.data) == 0
