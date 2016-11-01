from common_fixtures import *  # NOQA
from cattle import ApiError
from test_physical_host import disable_go_machine_service  # NOQA
from copy import deepcopy
from test_authorization import service_client  # NOQA


@pytest.fixture(scope='module')
def update_ping_settings(request, super_client):
    # These settings need changed because they control how the logic of the
    # ping handlers behave in cattle. We need to update them so that we can
    # ensure the ping logic will fully run.
    settings = super_client.list_setting()
    originals = []

    def update_setting(new_value, s):
        originals.append((setting, {'value': s.value}))
        s = super_client.update(s, {'value': new_value})
        wait_setting_active(super_client, s)

    for setting in settings:
        if setting.name == 'agent.ping.resources.every' and setting.value != 1:
            update_setting('1', setting)
        if setting.name == 'agent.resource.monitor.cache.resource.seconds' \
                and setting.value != 0:
            update_setting('0', setting)

    def revert_settings():
        for s in originals:
            super_client.update(s[0], s[1])

    request.addfinalizer(revert_settings)


FOO_DEFINITION = '''
    {
        "resourceFields": {
             "fooBar": {
                  "type": "string",
                  "create": true
             },
             "diskSize": {
                  "type": "string",
                  "create": true
             },
             "fooBaz": {
                  "type": "string",
                  "create": true
             }
        }
    }
    '''

BAR_DEFINITION = '''
    {
        "resourceFields": {
             "fooBar": {
                  "type": "string",
                  "create": true
             },
             "diskSize": {
                  "type": "string",
                  "create": true
             },
             "fooBaz": {
                  "type": "string",
                  "required": true,
                  "nullable": false,
                  "create": true
             }
        }
    }
    '''

MACHINE_DEFINITION = '''
{
    "collectionMethods":[
        "GET",
        "POST",
        "DELETE"
    ],
    "resourceMethods":[
        "GET",
        "PUT",
        "DELETE"
    ],
    "resourceFields":{
        "name":{
            "type":"string",
            "nullable":false,
            "minLength":1,
            "create":true
        },
        "authCertificateAuthority":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "fooConfig":{
            "type":"fooConfig",
            "nullable":true,
            "required":false,
            "create":true
        },
        "barConfig":{
            "type":"barConfig",
            "nullable":true,
            "required":false,
            "create":true
        },
        "authKey":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "labels":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineInstallUrl":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "dockerVersion":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "engineOpt":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineInsecureRegistry":{
            "type":"array[string]",
            "nullable":true,
            "create":true
        },
        "engineRegistryMirror":{
            "type":"array[string]",
            "nullable":true,
            "create":true
        },
        "engineLabel":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineStorageDriver":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "engineEnv":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        }
    }
}
'''

SUPER_MACHINE_DEFINITION = '''
{
    "collectionMethods":[
        "GET",
        "POST",
        "DELETE"
    ],
    "resourceMethods":[
        "GET",
        "PUT",
        "DELETE"
    ],
    "resourceFields":{
        "name":{
            "type":"string",
            "nullable":false,
            "minLength":1,
            "create":true
        },
        "authCertificateAuthority":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "fooConfig":{
            "type":"fooConfig",
            "nullable":true,
            "required":false,
            "create":true
        },
        "barConfig":{
            "type":"barConfig",
            "nullable":true,
            "required":false,
            "create":true
        },
        "authKey":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "labels":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineInstallUrl":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "dockerVersion":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "engineOpt":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineInsecureRegistry":{
            "type":"array[string]",
            "nullable":true,
            "create":true
        },
        "engineRegistryMirror":{
            "type":"array[string]",
            "nullable":true,
            "create":true
        },
        "engineLabel":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        },
        "engineStorageDriver":{
            "type":"string",
            "nullable":true,
            "create":true
        },
        "extractedConfig":{
            "type":"string",
            "nullable":true,
            "create":true,
            "update":true
        },
        "engineEnv":{
            "type":"map[string]",
            "nullable":true,
            "create":true
        }
    }
}
'''


MACHINE_READ_ONLY_DEFINITION = '''
{
    "collectionMethods":[
        "GET"
    ],
    "resourceMethods":[
        "GET"
    ],
    "resourceFields":{
        "name":{
            "type":"string",
            "nullable":false,
            "minLength":1
        },
        "authCertificateAuthority":{
            "type":"string",
            "nullable":true
        },
        "fooConfig":{
            "type":"fooConfig",
            "nullable":true,
            "required":false
        },
        "barConfig":{
            "type":"barConfig",
            "nullable":true,
            "required":false
        },
        "authKey":{
            "type":"string",
            "nullable":true
        },
        "labels":{
            "type":"map[string]",
            "nullable":true
        },
        "engineInstallUrl":{
            "type":"string",
            "nullable":true
        },
        "dockerVersion":{
            "type":"string",
            "nullable":true
        },
        "engineOpt":{
            "type":"map[string]",
            "nullable":true
        },
        "engineInsecureRegistry":{
            "type":"array[string]",
            "nullable":true
        },
        "engineRegistryMirror":{
            "type":"array[string]",
            "nullable":true
        },
        "engineLabel":{
            "type":"map[string]",
            "nullable":true
        },
        "engineStorageDriver":{
            "type":"string",
            "nullable":true
        },
        "engineEnv":{
            "type":"map[string]",
            "nullable":true
        }
    }
}
'''


def remove_schemas(service_client, schemas):  # NOQA
    for schema in schemas:
        got_schemas = service_client.list_dynamic_schema(name=schema)
        for got_schema in got_schemas:
            if got_schema.state != 'purged':
                service_client.wait_success(got_schema.remove())


def cleanup(service_client, schemas):  # NOQA
    remove_schemas(service_client, ['fooConfig', 'barConfig', 'machine',
                                    'host'])
    for schema in schemas:
        service_client.wait_success(
            service_client.create_dynamic_schema(schema))


@pytest.fixture(scope='module')  # NOQA
def machine_context(admin_user_client, service_client,  # NOQA
                    super_client, request):  # NOQA
    ctx = create_context(admin_user_client, create_project=True,
                         add_host=True)
    origMachineSchemas = service_client.list_dynamic_schema(name='machine')
    request.addfinalizer(
        lambda: cleanup(service_client, origMachineSchemas))
    remove_schemas(service_client, ['fooConfig', 'barConfig', 'machine',
                                    'host'])
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='fooConfig',
                                parent='baseMachineConfig',
                                definition=FOO_DEFINITION,
                                roles=['project', 'owner', 'member',
                                       'superadmin', 'readonly']))
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='barConfig',
                                parent='baseMachineConfig',
                                definition=BAR_DEFINITION,
                                roles=['project', 'owner', 'member',
                                       'superadmin', 'readonly']))
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='machine',
                                parent='physicalHost',
                                definition=MACHINE_DEFINITION,
                                roles=['project', 'owner', 'member']))
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='machine',
                                parent='physicalHost',
                                definition=SUPER_MACHINE_DEFINITION,
                                roles=['superadmin', 'admin']))
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='machine',
                                parent='physicalHost',
                                definition=MACHINE_READ_ONLY_DEFINITION,
                                roles=['readonly', 'agent']))
    service_client.wait_success(service_client.create_dynamic_schema(
        name='host',
        parent='host',
        definition=MACHINE_DEFINITION,
        roles=['project', 'owner', 'member']))
    service_client.wait_success(service_client.create_dynamic_schema(
        name='host',
        parent='host',
        definition=SUPER_MACHINE_DEFINITION,
        roles=['superadmin', 'admin']))
    service_client.wait_success(service_client.create_dynamic_schema(
                                name='host',
                                parent='host',
                                definition=MACHINE_READ_ONLY_DEFINITION,
                                roles=['readonly', 'agent']))
    super_client.reload_schema()
    ctx.client.reload_schema()
    return ctx


@pytest.mark.nonparallel
def test_host_lifecycle(super_client, machine_context, update_ping_settings):
    client = machine_context.client
    name = random_str()
    host = client.create_host(hostname=name, fooConfig={})
    host = wait_for_condition(client, host,
                              lambda x: x.physicalHostId is not None)
    machine = host.physicalHost()
    machine = machine_context.client.wait_success(machine)

    assert machine.state == 'active'
    assert machine.fooConfig is not None

    external_id = super_client.reload(machine).externalId
    assert external_id is not None

    # Create an agent with the externalId specified. The agent simulator will
    # mimic how the go-machine-service would use this external_id to bootstrap
    # an agent onto the physical host with the proper PHYSICAL_HOST_UUID set.
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    data = {scope: {}}
    data[scope]['addPhysicalHost'] = True
    data[scope]['externalId'] = external_id
    account_id = get_plain_id(super_client, machine_context.project)
    data[scope]['agentResourcesAccountId'] = account_id
    data['agentResourcesAccountId'] = account_id

    agent = super_client.create_agent(uri=uri, data=data)
    agent = super_client.wait_success(agent)

    wait_for(lambda: len(agent.hosts()) == 1)
    hosts = agent.hosts()

    assert len(hosts) == 1
    host = hosts[0].physicalHost()
    assert host.kind == 'machine'
    assert machine.accountId == host.accountId
    assert machine.uuid == host.uuid

    # Need to force a ping because they cause physical hosts to be created
    # under non-machine use cases. Ensures the machine isnt overridden
    ping = one(super_client.list_task, name='agent.ping')
    ping.execute()
    time.sleep(.1)  # The ping needs time to execute

    agent = super_client.reload(agent)
    hosts = agent.hosts()
    assert len(hosts) == 1
    host = hosts[0]
    physical_hosts = host.physicalHost()
    assert physical_hosts.id == machine.id

    machine = machine_context.client.wait_success(machine)
    host = machine_context.client.wait_success(host)

    agent = super_client.wait_success(agent)
    assert agent.state == 'active'

    host = machine_context.client.wait_success(machine_context
                                               .client.reload(host))
    host = super_client.reload(host)
    machine = super_client.reload(machine)
    assert host.physicalHostId == machine.id
    assert host.agentId == agent.id
    assert host.agentId == machine.agentId
    assert host.data.fields.reportedUuid == machine.externalId
    assert machine.name == name

    host = machine_context.client.wait_success(host.deactivate())
    host = machine_context.client.wait_success(host.remove())
    assert host.state == 'removed'
    machine = machine_context.client.wait_success(machine)
    assert machine.state == 'removed'


@pytest.mark.nonparallel
def test_machine_lifecycle(super_client, machine_context,
                           update_ping_settings, machine=None):
    if machine is None:
        machine = machine_context.client.create_machine(name=random_str(),
                                                        fooConfig={})

    machine = machine_context.client.wait_success(machine)
    assert machine.state == 'active'
    assert machine.fooConfig is not None

    external_id = super_client.reload(machine).externalId
    assert external_id is not None

    # Create an agent with the externalId specified. The agent simulator will
    # mimic how the go-machine-service would use this external_id to bootstrap
    # an agent onto the physical host with the proper PHYSICAL_HOST_UUID set.
    scope = 'io.cattle.platform.agent.connection.simulator' \
            '.AgentConnectionSimulator'
    uri = 'sim://{}'.format(random_str())
    data = {scope: {}}
    data[scope]['addPhysicalHost'] = True
    data[scope]['externalId'] = external_id
    account_id = get_plain_id(super_client, machine_context.project)
    data[scope]['agentResourcesAccountId'] = account_id
    data['agentResourcesAccountId'] = account_id

    agent = super_client.create_agent(uri=uri, data=data)
    agent = super_client.wait_success(agent)

    wait_for(lambda: len(agent.hosts()) == 1)
    hosts = agent.hosts()

    assert len(hosts) == 1
    host = hosts[0].physicalHost()
    assert host.kind == 'machine'
    assert machine.accountId == host.accountId
    assert machine.uuid == host.uuid

    # Need to force a ping because they cause physical hosts to be created
    # under non-machine use cases. Ensures the machine isnt overridden
    ping = one(super_client.list_task, name='agent.ping')
    ping.execute()
    time.sleep(.1)  # The ping needs time to execute

    agent = super_client.reload(agent)
    hosts = agent.hosts()
    assert len(hosts) == 1
    host = hosts[0]
    physical_hosts = host.physicalHost()
    assert physical_hosts.id == machine.id

    agent = super_client.reload(machine).agent()
    machine = machine_context.client.wait_success(machine.remove())
    assert machine.state == 'removed'

    agent = super_client.wait_success(agent)
    assert agent.removed is not None

    host = machine_context.client.wait_success(machine_context
                                               .client.reload(host))
    assert host.removed is not None


@pytest.mark.nonparallel
def test_machine_driver_config(machine_context):
    name = "test-%s" % random_str()
    foo_config = {
        "fooBar": "foo_string",
        "diskSize": "40000",
        "fooBaz": "http://localhost/random",
    }
    ca = "ca-1"
    key = "key-1"
    host = machine_context.client.create_machine(name=name,
                                                 fooConfig=foo_config,
                                                 authCertificateAuthority=ca,
                                                 authKey=key)
    host = machine_context.client.wait_success(host)
    assert host.state == 'active'
    assert foo_config['fooBar'] == host.fooConfig.fooBar
    assert foo_config['diskSize'] == host.fooConfig.diskSize
    assert foo_config['fooBaz'] == host.fooConfig.fooBaz
    assert ca == host.authCertificateAuthority
    assert key == host.authKey
    assert host.driver == 'foo'


@pytest.mark.nonparallel
def test_machine_validation(machine_context):
    name = "test-%s" % random_str()

    # Can't set two drivers
    with pytest.raises(ApiError) as e:
        machine_context.client.create_machine(name=name,
                                              fooConfig={},
                                              barConfig={"fooBaz": "fasd"})
    assert e.value.error.status == 422
    assert e.value.error.code == 'DriverConfigExactlyOneRequired'

    # Must set at least one driver
    with pytest.raises(ApiError) as e:
        machine_context.client.create_machine(name=name)
    assert e.value.error.status == 422
    assert e.value.error.code == 'DriverConfigExactlyOneRequired'

    # Property present, but None/nil/null is acceptable
    host = machine_context.client.create_machine(name=name,
                                                 fooConfig={},
                                                 barConfig=None)
    assert host is not None


@pytest.mark.nonparallel
def test_bar_config_machine(machine_context):
    name = "test-%s" % random_str()

    # accessToken is required
    with pytest.raises(ApiError) as e:
        machine_context.client.create_machine(name=name,
                                              barConfig={})
    assert e.value.error.status == 422
    assert e.value.error.code == 'MissingRequired'


@pytest.mark.nonparallel
def test_config_link_readonly(admin_user_client, super_client, request,
                              machine_context):
    user2_context = new_context(admin_user_client, request)

    project = machine_context.user_client.reload(machine_context.project)

    members = get_plain_members(project.projectMembers())
    members.append({
        'role': 'readonly',
        'externalId': user2_context.account.id,
        'externalIdType': 'rancher_id'
    })
    project.setmembers(members=members)

    user1_client = machine_context.user_client
    user2_client = user2_context.user_client

    new_headers = deepcopy(user1_client._headers)
    new_headers['X-API-Project-Id'] = project.id

    user1_client._headers = new_headers
    user2_client._headers = new_headers

    user1_client.reload_schema()
    user2_client.reload_schema()

    name = "test-%s" % random_str()
    foo_config = {
        "fooBar": "foo_string",
        "diskSize": "40000",
        "fooBaz": "http://localhost/random",
    }

    host = user1_client.create_machine(name=name,
                                       fooConfig=foo_config)
    host = user1_client.wait_success(host)
    host = super_client.by_id('physicalHost', host.id)

    super_client.update(host, extractedConfig='hello')

    host = super_client.reload(host)
    assert 'config' in host.links

    host = user1_client.reload(host)
    assert 'config' in host.links
    host = user2_client.reload(host)
    assert 'config' not in host.links

    super_client.update(host, extractedConfig='')
