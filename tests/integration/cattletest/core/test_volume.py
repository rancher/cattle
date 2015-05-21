from common_fixtures import *  # NOQA


def test_volume_delete_active(client, context):
    c = client.create_container(imageUuid=context.image_uuid)
    c = client.wait_success(c)
    assert c.state == 'running'

    volume = c.volumes()[0]
    assert volume.state == 'active'

    volume = client.wait_success(client.delete(volume))
    assert volume.state == 'removed'
