from common_fixtures import *  # NOQA


def test_zone_list(super_client, client):
    zones = super_client.list_zone()
    assert len(zones) > 0

    zones = client.list_zone()
    assert len(zones) >= 0
