from common_fixtures import *  # NOQA
from test_virtual_machine import *  # NOQA


def test_volume_auth(admin_client, client):
    auth_check(admin_client.schema, 'volume', 'rd', {
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

    auth_check(client.schema, 'volume', 'rd', {
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


def test_volume_delete_active(admin_client, sim_context):
    c = admin_client.create_container(imageUuid=sim_context['imageUuid'])
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    volume = c.volumes()[0]
    assert volume.state == 'active'

    volume = admin_client.wait_success(admin_client.delete(volume))
    assert volume.state == 'removed'
