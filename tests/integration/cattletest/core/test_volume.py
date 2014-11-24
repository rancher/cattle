from common_fixtures import *  # NOQA
from test_virtual_machine import *  # NOQA


def test_volume_auth(admin_client, client):
    auth_check(admin_client.schema, 'volume', 'r', {
        'accountId': 'r',
        'allocationState': 'r',
        'attachedState': 'r',
        'created': 'r',
        'data': 'r',
        'description': 'r',
        'deviceNumber': 'r',
        'format': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'name': 'r',
        'removeTime': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'virtualSizeMb': 'r',
        'zoneId': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r'
    })

    auth_check(client.schema, 'volume', 'r', {
        'created': 'r',
        'description': 'r',
        'deviceNumber': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'name': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'virtualSizeMb': 'r',
        'zoneId': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r'
    })
