from common_fixtures import *  # NOQA


TYPE_LEN = {
    'admin': 98,
    'agent': 8,
    'user': 76,
    'agentRegister': 4,
    'readAdmin': 98,
    'token': 2,
    'superadmin': 152,
    'service': 98,
    'project': 76,
}


@pytest.fixture(scope='module')
def clients(super_client, admin_user_client):
    ret = {}
    for k in TYPE_LEN.keys():
        name = 'auth test {}'.format(k)
        accounts = super_client.list_account(name=name)
        if len(accounts) == 1:
            api_keys = filter(lambda x: x.kind == 'apiKey',
                              accounts[0].credentials())
            if len(api_keys) == 1:
                ret[k] = api_client(api_keys[0].publicValue,
                                    api_keys[1].secretValue)
                continue

        context = create_context(admin_user_client, kind=k)
        ret[k] = context.user_client

    return ret


@pytest.fixture(scope='module')
def user_client(clients):
    return clients['user']


@pytest.fixture(scope='module')
def project_client(clients):
    return clients['project']


@pytest.fixture(scope='module')
def token_client(clients):
    return clients['token']


@pytest.fixture(scope='module')
def agent_client(clients):
    return clients['agent']


@pytest.fixture(scope='module')
def service_client(clients):
    return clients['service']


def test_user_client_access(clients):
    for kind, client in clients.items():
        assert TYPE_LEN[kind] == len(client.schema.types.items())


def test_instance_link_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'instanceLink', 'r', {
        'accountId': 'r',
        'data': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'ports': 'r',
        'targetInstanceId': 'r',
    })

    auth_check(user_client.schema, 'instanceLink', 'r', {
        'accountId': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'targetInstanceId': 'r',
    })

    auth_check(project_client.schema, 'instanceLink', 'ru', {
        'accountId': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'targetInstanceId': 'ru',
    })


def test_token_auth(token_client):
    auth_check(token_client.schema, 'token', 'cr', {
        'jwt': 'r',
        'code': 'cr',
        'user': 'r',
        'orgs': 'r',
        'clientId': 'r',
        'security': 'r',
        'teams': 'r',
        'userType': 'r',
        'accountId': 'r'
    })


def test_github_auth(admin_user_client, user_client, project_client):
    assert 'githubconfig' not in user_client.schema.types
    assert 'githubconfig' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'githubconfig', 'cru', {
        'enabled': 'cr',
        'allowedOrganizations': 'cr',
        'allowedUsers': 'cr',
        'clientId': 'cr',
        'clientSecret': 'cr',
        'accessMode': 'cr'
    })


def test_project_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'project', 'crud', {
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'cr',
        'data': 'r',
        'members': 'cr'
    })

    auth_check(user_client.schema, 'project', 'crud', {
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'r',
        'members': 'cr'
    })

    auth_check(project_client.schema, 'project', 'r', {
        'description': 'r',
        'kind': 'r',
        'name': 'r',
        'uuid': 'r',
        'members': 'r'
    })


def test_project_member_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'projectMember', 'cr', {
        "name": "r",
        "role": "cr",
        "externalId": "cr",
        "externalIdType": "cr",
        "projectId": "r",
        "data": 'r'
    })

    auth_check(user_client.schema, 'projectMember', 'cr', {
        "name": "r",
        "role": "cr",
        "externalId": "cr",
        "externalIdType": "cr",
        "projectId": "r"
    })

    auth_check(project_client.schema, 'projectMember', 'r', {
        "name": "r",
        "role": "r",
        "externalId": "r",
        "externalIdType": "r",
        "projectId": "r"
    })


def test_host_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'host', 'r', {
        'accountId': 'r',
        'apiProxy': 'r',
        'agentId': 'r',
        'computeTotal': 'r',
        'data': 'r',
        'physicalHostId': 'r',
        'info': 'r',
    })

    auth_check(user_client.schema, 'host', 'r', {
        'accountId': 'r',
        'computeTotal': 'r',
        'physicalHostId': 'r',
        'info': 'r',
    })

    auth_check(project_client.schema, 'host', 'rud', {
        'accountId': 'r',
        'computeTotal': 'r',
        'physicalHostId': 'r',
        'info': 'r',
    })


def test_ip_address_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'networkId': 'r',
        'address': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'address': 'r',
        'networkId': 'r',
    })

    auth_check(project_client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'address': 'r',
        'networkId': 'r',
    })


def test_task_instance_auth(admin_user_client, user_client, project_client):
    assert 'taskInstance' not in user_client.schema.types
    assert 'taskInstance' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'taskInstance', 'r', {
        'endTime': 'r',
        'exception': 'r',
        'serverId': 'r',
        'startTime': 'r',
        'taskId': 'r',
    })


def test_volume_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'volume', 'r', {
        'accountId': 'r',
        'created': 'r',
        'data': 'r',
        'description': 'r',
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
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r'
    })

    auth_check(user_client.schema, 'volume', 'r', {
        'accountId': 'r',
        'created': 'r',
        'description': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'name': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r'
    })

    auth_check(project_client.schema, 'volume', 'rd', {
        'accountId': 'r',
        'created': 'r',
        'description': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'name': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r'
    })


def test_container_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'container', 'r', {
        'accountId': 'r',
        'agentId': 'r',
        'allocationState': 'r',
        'capAdd': 'r',
        'capDrop': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'data': 'r',
        'dataVolumes': 'r',
        'dataVolumesFrom': 'r',
        'description': 'r',
        'devices': 'r',
        'directory': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'entryPoint': 'r',
        'environment': 'r',
        'firstRunning': 'r',
        'hostname': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'lxcConf': 'r',
        'memory': 'r',
        'memorySwap': 'r',
        'networkIds': 'r',
        'networkMode': 'r',
        'networkContainerId': 'r',
        'ports': 'r',
        'primaryIpAddress': 'r',
        'privileged': 'r',
        'publishAllPorts': 'r',
        'removeTime': 'r',
        'registryCredentialId': 'r',
        'requestedHostId': 'r',
        'restartPolicy': 'r',
        'startOnCreate': 'r',
        'stdinOpen': 'r',
        'token': 'r',
        'tty': 'r',
        'user': 'r',
        'systemContainer': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r'
    })

    auth_check(user_client.schema, 'container', 'r', {
        'accountId': 'r',
        'capAdd': 'r',
        'capDrop': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'dataVolumes': 'r',
        'dataVolumesFrom': 'r',
        'description': 'r',
        'devices': 'r',
        'directory': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'entryPoint': 'r',
        'environment': 'r',
        'firstRunning': 'r',
        'hostname': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'lxcConf': 'r',
        'memory': 'r',
        'memorySwap': 'r',
        'networkIds': 'r',
        'networkMode': 'r',
        'networkContainerId': 'r',
        'ports': 'r',
        'primaryIpAddress': 'r',
        'privileged': 'r',
        'publishAllPorts': 'r',
        'registryCredentialId': 'r',
        'requestedHostId': 'r',
        'restartPolicy': 'r',
        'startOnCreate': 'r',
        'stdinOpen': 'r',
        'tty': 'r',
        'user': 'r',
        'systemContainer': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r'
    })

    auth_check(project_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'description': 'cru',
        'devices': 'cr',
        'directory': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domainName': 'cr',
        'entryPoint': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'lxcConf': 'cr',
        'memory': 'cr',
        'memorySwap': 'cr',
        'networkIds': 'cr',
        'networkMode': 'cr',
        'networkContainerId': 'cr',
        'ports': 'cr',
        'primaryIpAddress': 'r',
        'privileged': 'cr',
        'publishAllPorts': 'cr',
        'registryCredentialId': 'cr',
        'requestedHostId': 'cr',
        'restartPolicy': 'cr',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'tty': 'cr',
        'user': 'cr',
        'systemContainer': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'cr'
    })


def test_port_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'port', 'r', {
        'accountId': 'r',
        'data': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'r',
    })

    auth_check(user_client.schema, 'port', 'r', {
        'accountId': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'r',
    })

    auth_check(project_client.schema, 'port', 'ru', {
        'accountId': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'ru',
    })


def test_mount_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'mount', 'r', {
        'name': 'r',
        'description': 'r',
        'data': 'r',
        'accountId': 'r',
        'instanceId': 'r',
        'volumeId': 'r',
        'kind': 'r',
        'uuid': 'r',
        'removeTime': 'r',
        'id': 'r',
        'created': 'r',
        'path': 'r',
        'permissions': 'r',
        'removed': 'r',
        'state': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r'
    })

    auth_check(user_client.schema, 'mount', 'r', {
        'accountId': 'r',
        'name': 'r',
        'description': 'r',
        'instanceId': 'r',
        'volumeId': 'r',
        'kind': 'r',
        'uuid': 'r',
        'id': 'r',
        'created': 'r',
        'path': 'r',
        'permissions': 'r',
        'removed': 'r',
        'state': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r'
    })

    auth_check(project_client.schema, 'mount', 'r', {
        'accountId': 'r',
        'name': 'r',
        'description': 'r',
        'instanceId': 'r',
        'volumeId': 'r',
        'kind': 'r',
        'uuid': 'r',
        'id': 'r',
        'created': 'r',
        'path': 'r',
        'permissions': 'r',
        'removed': 'r',
        'state': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r'
    })


def test_process_instance_auth(admin_user_client, user_client, project_client):
    assert 'processInstance' not in user_client.schema.types
    assert 'processInstance' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'processInstance', 'r', {
        'endTime': 'r',
        'exitReason': 'r',
        'phase': 'r',
        'priority': 'r',
        'processName': 'r',
        'resourceId': 'r',
        'resourceType': 'r',
        'result': 'r',
        'runningProcessServerId': 'r',
        'startProcessServerId': 'r',
        'startTime': 'r',
        'data': 'r',
    })


def test_process_execution(admin_user_client, user_client, project_client):
    assert 'processExecution' not in user_client.schema.types
    assert 'processExecution' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'processExecution', 'r', {
        'log': 'r',
        'processInstanceId': 'r',
    })


def test_process_definition(admin_user_client, user_client, project_client):
    assert 'processDefinition' not in user_client.schema.types
    assert 'processDefinition' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'processDefinition', 'r', {
        'extensionBased': 'r',
        'preProcessListeners': 'r',
        'postProcessListeners': 'r',
        'processHandlers': 'r',
        'resourceType': 'r',
        'stateTransitions': 'r',
    })


def test_config_item(admin_user_client, user_client, project_client):
    assert 'configItem' not in user_client.schema.types
    assert 'configItem' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'configItem', 'r', {
        'sourceVersion': 'r',
    })


def test_config_item_status_auth(admin_user_client, user_client,
                                 project_client):
    assert 'configItemStatus' not in user_client.schema.types
    assert 'configItemStatus' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'configItemStatus', 'ru', {
        'agentId': 'r',
        'appliedUpdated': 'r',
        'appliedVersion': 'ru',
        'requestedUpdated': 'r',
        'requestedVersion': 'r',
        'sourceVersion': 'r',
    })


def test_setting_auth(admin_user_client, user_client, project_client):
    assert 'setting' not in user_client.schema.types
    assert 'setting' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'setting', 'crud', {
        'name': 'cr',
        'value': 'cru',
    })


def test_schema_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'schema', 'r', {
        'collectionActions': 'r',
        'collectionFields': 'r',
        'collectionFilters': 'r',
        'collectionMethods': 'r',
        'includeableLinks': 'r',
        'pluralName': 'r',
        'resourceActions': 'r',
        'resourceFields': 'r',
        'resourceMethods': 'r',
    })

    auth_check(user_client.schema, 'schema', 'r', {
        'collectionActions': 'r',
        'collectionFields': 'r',
        'collectionFilters': 'r',
        'collectionMethods': 'r',
        'includeableLinks': 'r',
        'pluralName': 'r',
        'resourceActions': 'r',
        'resourceFields': 'r',
        'resourceMethods': 'r',
    })

    auth_check(project_client.schema, 'schema', 'r', {
        'collectionActions': 'r',
        'collectionFields': 'r',
        'collectionFilters': 'r',
        'collectionMethods': 'r',
        'includeableLinks': 'r',
        'pluralName': 'r',
        'resourceActions': 'r',
        'resourceFields': 'r',
        'resourceMethods': 'r',
    })


def test_account_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'account', 'crud', {
        'id': 'r',
        'externalId': 'cru',
        'externalIdType': 'cru',
        'removeTime': 'r',
        'data': 'r',
        'kind': 'cru',
        'uuid': 'cr'
    })

    auth_check(user_client.schema, 'account', 'r', {
    })

    auth_check(project_client.schema, 'account', 'r', {
    })


def test_agent_auth(admin_user_client, user_client, project_client):
    assert 'agent' not in user_client.schema.types
    assert 'agent' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'agent', 'r', {
        'managedConfig': 'r',
        'uri': 'r',
        'accountId': 'r',
        'data': 'r',
    })


def test_extension_point_auth(admin_user_client, user_client, project_client):
    assert 'extensionPoint' not in user_client.schema.types
    assert 'extensionPoint' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'extensionPoint', 'r', {
        'excludeSetting': 'r',
        'includeSetting': 'r',
        'listSetting': 'r',
        'implementations': 'r',
    })


def test_api_key_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'apiKey', 'crud', {
        'publicValue': 'cr',
        'secretValue': 'cr',
        'removeTime': 'r',
        'data': 'r',
        'accountId': 'cr',
    })

    auth_check(user_client.schema, 'apiKey', 'r', {
        'publicValue': 'r',
        'accountId': 'r',
        'secretValue': 'r',
    })

    auth_check(project_client.schema, 'apiKey', 'crud', {
        'publicValue': 'r',
        'accountId': 'r',
        'secretValue': 'r',
    })


def test_subscribe_auth(admin_user_client, user_client, project_client):
    assert 'subscribe' not in admin_user_client.schema.types
    assert 'subscribe' not in user_client.schema.types

    auth_check(project_client.schema, 'subscribe', 'cr', {
        'eventNames': 'cr',
    })


def test_registration_tokens_auth(admin_user_client, user_client,
                                  project_client, service_client):
    auth_check(admin_user_client.schema, 'registrationToken', 'r', {
        'created': 'r',
        'data': 'r',
        'description': 'r',
        'removeTime': 'r',
        'accountId': 'r',
        'image': 'r',
        'command': 'r',
        'registrationUrl': 'r',
        'token': 'r',
    })

    auth_check(service_client.schema, 'registrationToken', 'cr', {
        'created': 'r',
        'data': 'r',
        'description': 'cr',
        'removeTime': 'r',
        'accountId': 'cr',
        'image': 'r',
        'command': 'r',
        'registrationUrl': 'r',
        'token': 'r',
    })

    auth_check(user_client.schema, 'registrationToken', 'r', {
        'accountId': 'r',
        'created': 'r',
        'description': 'r',
        'uuid': 'r',
        'image': 'r',
        'command': 'r',
        'registrationUrl': 'r',
        'token': 'r',
    })

    auth_check(project_client.schema, 'registrationToken', 'cr', {
        'accountId': 'r',
        'created': 'r',
        'description': 'cr',
        'uuid': 'r',
        'image': 'r',
        'command': 'r',
        'registrationUrl': 'r',
        'token': 'r',
    })


def test_type_documentation_auth(admin_user_client, user_client,
                                 project_client):
    auth_check(admin_user_client.schema, 'typeDocumentation', 'r', {
    })

    auth_check(user_client.schema, 'typeDocumentation', 'r', {
    })

    auth_check(project_client.schema, 'typeDocumentation', 'r', {
    })


def test_stats_access_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'statsAccess', 'r', {
        'token': 'r',
        'url': 'r',
    })

    auth_check(user_client.schema, 'statsAccess', 'r', {
        'token': 'r',
        'url': 'r',
    })

    auth_check(project_client.schema, 'statsAccess', 'r', {
        'token': 'r',
        'url': 'r',
    })


def test_account_resource_auth(admin_user_client):
    resource_action_check(admin_user_client.schema, 'account', [
        'update',
        'activate',
        'deactivate',
        'restore',
        'remove',
        'purge',
        'create'
    ])


def test_machine(admin_user_client, user_client, service_client,
                 project_client):
    auth_check(admin_user_client.schema, 'machine', 'r', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'data': 'r',
        'authCertificateAuthority': 'r',
        'authKey': 'r',
        'virtualboxConfig': 'r',
        'digitaloceanConfig': 'r',
        'amazonec2Config': 'r',
        'rackspaceConfig': 'r',
        'packetConfig': 'r',
    })

    auth_check(user_client.schema, 'machine', 'r', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'authCertificateAuthority': 'r',
        'authKey': 'r',
        'virtualboxConfig': 'r',
        'digitaloceanConfig': 'r',
        'amazonec2Config': 'r',
        'rackspaceConfig': 'r',
        'packetConfig': 'r',
    })

    auth_check(project_client.schema, 'machine', 'crd', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'virtualboxConfig': 'cr',
        'digitaloceanConfig': 'cr',
        'amazonec2Config': 'cr',
        'rackspaceConfig': 'cr',
        'packetConfig': 'cr',
        })

    auth_check(service_client.schema, 'machine', 'crud', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'data': 'cru',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'extractedConfig': 'ru',
        'virtualboxConfig': 'cr',
        'digitaloceanConfig': 'cr',
        'amazonec2Config': 'cr',
        'rackspaceConfig': 'cr',
        'packetConfig': 'cr',
    })


def test_physical_host(admin_user_client, user_client, service_client,
                       project_client):
    auth_check(admin_user_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
    })

    auth_check(project_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
    })


def test_registry_credentials(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'registryCredential', 'r', {
        'accountId': 'r',
        'data': 'r',
        'email': 'r',
        'publicValue': 'r',
        'secretValue': 'r',
        'registryId': 'r',
    })

    auth_check(user_client.schema, 'registryCredential', 'r', {
        'accountId': 'r',
        'email': 'r',
        'publicValue': 'r',
        'secretValue': 'r',
        'registryId': 'r',
    })

    auth_check(project_client.schema, 'registryCredential', 'crud', {
        'accountId': 'r',
        'email': 'cru',
        'publicValue': 'cru',
        'secretValue': 'cru',
        'registryId': 'cr',
    })


def test_registry(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'registry', 'r', {
        'accountId': 'r',
        'data': 'r',
        'serverAddress': 'r',
    })

    auth_check(user_client.schema, 'registry', 'r', {
        'accountId': 'r',
        'serverAddress': 'r',
    })

    auth_check(project_client.schema, 'registry', 'crud', {
        'accountId': 'r',
        'serverAddress': 'cr',
    })


def test_lb_config_listener_map(admin_user_client, user_client,
                                project_client):
    auth_check(admin_user_client.schema, 'loadBalancerConfigListenerMap', 'r',
               {
                   'loadBalancerConfigId': 'r',
                   'loadBalancerListenerId': 'r',
                   'accountId': 'r',
                   'data': 'r',
               })

    auth_check(user_client.schema, 'loadBalancerConfigListenerMap', 'r', {
        'loadBalancerConfigId': 'r',
        'loadBalancerListenerId': 'r',
        'accountId': 'r',
    })

    auth_check(project_client.schema, 'loadBalancerConfigListenerMap', 'r', {
        'loadBalancerConfigId': 'r',
        'loadBalancerListenerId': 'r',
        'accountId': 'r',
    })


def test_lb_host_map(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'loadBalancerHostMap', 'r', {
        'hostId': 'r',
        'loadBalancerId': 'r',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'loadBalancerHostMap', 'r', {
        'hostId': 'r',
        'loadBalancerId': 'r',
        'accountId': 'r',
    })

    auth_check(project_client.schema, 'loadBalancerHostMap', 'r', {
        'hostId': 'r',
        'loadBalancerId': 'r',
        'accountId': 'r',
    })


def test_container_events(admin_user_client, user_client, agent_client,
                          project_client):
    auth_check(admin_user_client.schema, 'containerEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'accountId': 'r',
        'externalFrom': 'r',
        'reportedHostUuid': 'r',
        'externalId': 'r',
        'externalStatus': 'r',
        'data': 'r',
        'dockerInspect': 'r'
    })

    auth_check(agent_client.schema, 'containerEvent', 'cr', {
        'externalTimestamp': 'cr',
        'externalFrom': 'cr',
        'reportedHostUuid': 'cr',
        'externalId': 'cr',
        'externalStatus': 'cr',
        'dockerInspect': 'cr',
        'data': 'cr',
        'id': 'r'
    })

    auth_check(user_client.schema, 'containerEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'externalFrom': 'r',
        'reportedHostUuid': 'r',
        'externalId': 'r',
        'externalStatus': 'r',
        'accountId': 'r',
        'dockerInspect': 'r'
    })

    auth_check(project_client.schema, 'containerEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'externalFrom': 'r',
        'reportedHostUuid': 'r',
        'externalId': 'r',
        'externalStatus': 'r',
        'accountId': 'r',
        'dockerInspect': 'r'
    })


def test_svc_discovery_service(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'service', 'r', {
        'name': 'r',
        'environmentId': 'r',
        'scale': 'r',
        'dataVolumesFromService': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'service', 'r', {
        'name': 'r',
        'environmentId': 'r',
        'scale': 'r',
        'dataVolumesFromService': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
    })

    auth_check(project_client.schema, 'service', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
    })


def test_svc_discovery_environment(admin_user_client, user_client,
                                   project_client):
    auth_check(admin_user_client.schema, 'environment', 'r', {
        'name': 'r',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'environment', 'r', {
        'name': 'r',
        'accountId': 'r',
    })

    auth_check(project_client.schema, 'environment', 'crud', {
        'name': 'cru',
        'accountId': 'r',
    })


def test_svc_discovery_lb_service(admin_user_client, user_client,
                                  project_client):
    auth_check(admin_user_client.schema, 'loadBalancerService', 'r', {
        'name': 'r',
        'environmentId': 'r',
        'scale': 'r',
        'dataVolumesFromService': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'data': 'r',
        'loadBalancerConfig': 'r',
    })

    auth_check(user_client.schema, 'loadBalancerService', 'r', {
        'name': 'r',
        'environmentId': 'r',
        'scale': 'r',
        'dataVolumesFromService': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'loadBalancerConfig': 'r',
    })

    auth_check(project_client.schema, 'loadBalancerService', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
        'loadBalancerConfig': 'cr',
    })
