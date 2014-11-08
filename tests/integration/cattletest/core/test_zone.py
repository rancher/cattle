from common_fixtures import *  # NOQA


def test_zone_list(admin_client, client):
    zones = admin_client.list_zone()
    assert len(zones) > 0

    zones = client.list_zone()
    assert len(zones) >= 0
