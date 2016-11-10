from common import *  # NOQA
from gdapi import ApiError
from test_authorization import user_client  # NOQA
from test_authorization import service_client  # NOQA
from test_authorization import project_client  # NOQA

DEFINITION = '''
    {
        "resourceFields": {
             "fooBar": {
                  "type": "string",
                  "description": "foo animal"
             }
        }
    }
    '''


def test_schema_lifecycle(context, client, service_client):  # NOQA
    schema = random_str() + 'Config'
    got_schema = client.by_id_schema(schema)
    assert got_schema is None

    made_schema = service_client.create_dynamic_schema(
        accountId=context.project.id,
        name=schema,
        parent='service',
        definition=DEFINITION,
        roles=["project", "owner", "member"])

    service_client.wait_success(made_schema)

    got_schema = client.by_id_schema(schema)
    assert got_schema is not None

    assert got_schema.resourceFields.fooBar.type == 'string'
    assert got_schema.resourceFields.fooBar.description == 'foo animal'

    made_schema = service_client.reload(made_schema)
    made_schema.remove()
    service_client.wait_success(made_schema)

    got_schema = client.by_id_schema(schema)
    assert got_schema is None


def test_invalid_schema_definition(context, client, service_client):  # NOQA
    with pytest.raises(ApiError) as e:
        service_client.create_dynamic_schema(
            accountId=context.project.id,
            name=random_str(),
            parent='service',
            definition='{"fsadfasd":"fasdfdsf",}',
            roles=["project", "owner", "member"])
    assert e.value.error.status == 422
    assert e.value.error.fieldName == 'definition'


def test_schema_roles(service_client, user_client, project_client):  # NOQA
    schema = random_str()
    got_schema = project_client.by_id_schema(schema)
    assert got_schema is None

    made_schema = service_client.create_dynamic_schema(
        name=schema,
        parent='baseMachineConfig',
        definition='''
        {
            "resourceFields": {
                 "fooBar": {
                      "type": "string",
                      "description": "foo animal"
                 }
            }
        }
        ''',
        roles=["project"])

    service_client.wait_success(made_schema)

    project_client.reload_schema()

    auth_check(project_client.schema, schema, 'r', {
        'fooBar': 'r'
    })

    made_schema = service_client.reload(made_schema)
    made_schema.remove()
    service_client.wait_success(made_schema)

    got_schema = project_client.by_id_schema(schema)
    assert got_schema is None

    made_schema2 = service_client.create_dynamic_schema(
        name=schema,
        parent='baseMachineConfig',
        definition='''
        {
            "resourceMethods" : ["GET", "PUT", "DELETE"],
            "collectionMethods" : [ "GET", "POST" ],
            "resourceFields": {
                 "fooBar": {
                      "type": "string",
                      "description": "foo animal",
                      "create": true,
                      "update": true
                 }
            }
        }
        ''',
        roles=["user"])

    service_client.wait_success(made_schema2)

    user_client.reload_schema()

    auth_check(user_client.schema, schema, 'crud', {
        'fooBar': 'cru'
    })

    made_schema2 = service_client.reload(made_schema2)
    made_schema2.remove()
    service_client.wait_success(made_schema2)

    got_schema = user_client.by_id_schema(schema)
    assert got_schema is None
