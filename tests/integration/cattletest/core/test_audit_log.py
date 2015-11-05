from common_fixtures import *  # NOQA
from copy import deepcopy


def made_log(object, admin_user_client, context):
    logs = context.client.list_audit_log(resourceId=object.id,
                                         resourceType=object.type)
    assert len(logs) == 1
    assert logs[0].resourceType == object.type
    assert "{}".format(logs[0].resourceId) == object.id
    assert logs[0].accountId == context.project.id
    assert logs[0].authenticatedAsAccountId == context.account.id


def test_audit_entry_created(new_context, admin_user_client):
    objects = []
    new_headers = deepcopy(new_context.user_client._headers)
    new_headers['X-API-Project-Id'] = new_context.project.id

    new_context.user_client._headers = new_headers
    new_context.user_client.reload_schema()
    objects.append(new_context.user_client.create_container(
        imageUuid=new_context.image_uuid))
    objects.append(new_context.user_client.create_container(
        imageUuid=new_context.image_uuid))
    objects.append(new_context.user_client.create_api_key())
    objects.append(new_context.user_client.create_registry(
        serverAddress='test.io', name='test'))
    objects.append(new_context.user_client.create_api_key())
    for object in objects:
        made_log(object, admin_user_client, new_context)
