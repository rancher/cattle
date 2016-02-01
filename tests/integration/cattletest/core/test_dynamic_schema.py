from common_fixtures import *  # NOQA
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
    schema = random_str()
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


def test_service_with_schema(new_context, super_client):
    client = new_context.client
    env = client.wait_success(client.create_environment(name='test'))
    assert env.state == 'active'

    service = client.create_service(environmentId=env.id,
                                    name='test',
                                    launchConfig={
                                        'imageUuid': new_context.image_uuid
                                    },
                                    serviceSchemas={
                                        'container': {
                                            'resourceFields': {
                                                'fooBar': {
                                                    'type': 'string',
                                                    'create': True
                                                }
                                            }
                                        },
                                        'lala': {
                                            'resourceFields': {
                                                'fooBar': {
                                                    'type': 'string',
                                                    'create': True
                                                }
                                            }
                                        }
                                    })
    service = client.wait_success(service)
    assert service.state == 'inactive'

    lala = client.by_id_schema('lala')
    assert lala is not None

    assert lala.resourceFields.fooBar.create is True
    assert lala.resourceFields.scale.type == 'int'
    assert lala.links.collection is not None

    container_schema = client.by_id_schema('container')
    assert 'fooBar' not in container_schema.resourceFields

    dynamic_schema = find_one(super_client.list_dynamic_schema,
                              serviceId=service.id)

    assert dynamic_schema.name == 'lala'
    assert dynamic_schema.parent == 'service'

    s = super_client.create_service(environmentId=env.id,
                                    kind='lala',
                                    name='test' + random_str(),
                                    accountId=dynamic_schema.accountId,
                                    launchConfig={
                                        'imageUuid': new_context.image_uuid
                                    })

    s = client.reload(s)
    assert s.kind == 'lala'
    assert s.type == 'lala'

    client.delete(service)
    service = client.wait_success(service)

    assert service.state == 'removed'

    dynamic_schema = super_client.reload(dynamic_schema)
    assert dynamic_schema is None
