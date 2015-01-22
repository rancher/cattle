import websocket as ws

from cattle import ApiError
from common_fixtures import *


def test_host_api_token(admin_client):
    hosts = admin_client.list_host(kind='docker', removed_null=True)
    assert len(hosts) > 0

    stats = hosts[0].stats()
    conn = ws.create_connection(stats.url + '?token=' + stats.token)
    result = conn.recv()
    assert result is not None


def test_host_api_no_token(admin_client):
    hosts = admin_client.list_host(kind='docker', removed_null=True)
    assert len(hosts) > 0

    stats = hosts[0].stats()
    conn = ws.create_connection(stats.url)
    assert conn.recv() is None


def test_host_api_garbage_token(admin_client):
    hosts = admin_client.list_host(kind='docker', removed_null=True)
    assert len(hosts) > 0

    stats = hosts[0].stats()
    conn = ws.create_connection(stats.url + '?token=abcd')
    assert conn.recv() is None