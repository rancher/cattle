from common_fixtures import *  # NOQA

DEFINITION = '''
    {
        "id": "lala",
        "resourceFields": {
             "fooBar": {
                  "type": "string"
             }
        }
    }
    '''


def test_schema_create(context, client, super_client):
    lala = client.by_id_schema('lala')
    assert lala is None

    super_client.create_dynamic_schema(accountId=context.project.id,
                                       name='lala',
                                       parent='service',
                                       definition=DEFINITION)

    lala = client.by_id_schema('lala')
    assert lala is not None

    assert lala.resourceFields.fooBar.type == 'string'


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
