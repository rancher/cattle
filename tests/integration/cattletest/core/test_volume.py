from common_fixtures import *  # NOQA
from test_virtual_machine import *  # NOQA


def test_volume_delete_active(admin_client, sim_context):
    c = admin_client.create_container(
        imageUuid=sim_context['imageUuid'])
    c = admin_client.wait_success(c)
    assert c.state == 'running'

    volume = c.volumes()[0]
    assert volume.state == 'active'

    volume = admin_client.wait_success(
        admin_client.delete(volume))
    assert volume.state == 'removed'
