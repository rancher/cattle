from common_fixtures import *  # NOQA


def test_instance_link_auth(admin_client, client):
    auth_check(admin_client.schema, 'instanceLink', 'ru', {
        'accountId': 'ru',
        'data': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'ports': 'r',
        'removeTime': 'ru',
        'targetInstanceId': 'ru',
    })

    auth_check(client.schema, 'instanceLink', 'ru', {
        'accountId': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'targetInstanceId': 'ru',
    })


def test_token_client(token_client):
    auth_check(token_client.schema, 'token', 'cr', {
        'jwt': 'r',
        'code': 'cr',
        'clientId': 'r',
        'orgs': 'r',
        'security': 'r',
        'user': 'r'
    })


def test_github_auth(admin_client):
    auth_check(admin_client.schema, 'githubconfig', 'crud', {
        'enabled': 'cr',
        'allowedOrganizations': 'cr',
        'allowedUsers': 'cr',
        'clientId': 'cr'
    })


def test_host_auth(admin_client, client):
    auth_check(admin_client.schema, 'host', 'rud', {
        'accountId': 'r',
        'agentId': 'r',
        'computeTotal': 'r',
        'zoneId': 'ru',
        'isPublic': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'host', 'rud', {
        'accountId': 'r',
        'computeTotal': 'r',
        'zoneId': 'ru',
        'agentId': 'r',
    })


def test_ip_address_auth(admin_client, client):
    auth_check(admin_client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'address': 'r',
    })

    auth_check(client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'address': 'r',
    })


def test_task_instance_auth(admin_client, client):
    auth_check(admin_client.schema, 'taskInstance', 'r', {
        'endTime': 'r',
        'exception': 'r',
        'serverId': 'r',
        'startTime': 'r',
        'taskId': 'r',
        })


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
        'accountId': 'r',
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


def test_container_auth(admin_client, client):
    auth_check(admin_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'agentId': 'r',
        'allocationState': 'r',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'commandArgs': 'cr',
        'compute': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'credentialIds': 'cr',
        'data': 'r',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'description': 'cru',
        'devices': 'cr',
        'directory': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domain': 'cr',
        'domainName': 'cr',
        'entryPoint': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'instanceTriggeredStop': 'cru',
        'lxcConf': 'cr',
        'memory': 'cr',
        'memorySwap': 'cr',
        'networkIds': 'cr',
        'ports': 'cr',
        'primaryIpAddress': 'r',
        'privileged': 'cr',
        'publishAllPorts': 'cr',
        'removeTime': 'ru',
        'requestedHostId': 'cr',
        'restartPolicy': 'cr',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'token': 'r',
        'tty': 'cr',
        'user': 'cr',
    })

    auth_check(client.schema, 'container', 'crud', {
        'accountId': 'r',
        'agentId': 'r',
        'command': 'cr',
        'commandArgs': 'cr',
        'count': 'cr',
        'created': 'r',
        'credentialIds': 'cr',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'description': 'cru',
        'directory': 'cr',
        'domain': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'networkIds': 'cr',
        'ports': 'cr',
        'primaryIpAddress': 'r',
        'privileged': 'cr',
        'publishAllPorts': 'cr',
        'requestedHostId': 'cr',
        'startOnCreate': 'cr',
        'user': 'cr',
    })


def test_port_auth(admin_client, client):
    auth_check(admin_client.schema, 'port', 'ru', {
        'accountId': 'r',
        'data': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'ru',
        'removeTime': 'ru',
    })

    auth_check(client.schema, 'port', 'ru', {
        'accountId': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'ru',
    })


def test_mount_auth(admin_client, client):
    auth_check(admin_client.schema, 'mount', 'r', {
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

    auth_check(client.schema, 'mount', 'r', {
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


def test_process_instance_auth(admin_client, client):
    auth_check(admin_client.schema, 'processInstance', 'r', {
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

    auth_check(client.schema, 'processInstance', 'r', {
    })


def test_process_execution(admin_client, client):
    auth_check(admin_client.schema, 'processExecution', 'r', {
        'log': 'r',
        'processInstanceId': 'r',
    })

    auth_check(client.schema, 'processExecution', 'r', {

    })


def test_process_definition(admin_client, client):
    auth_check(admin_client.schema, 'processDefinition', 'r', {
        'extensionBased': 'r',
        'preProcessListeners': 'r',
        'postProcessListeners': 'r',
        'processHandlers': 'r',
        'resourceType': 'r',
    })


def test_config_item(admin_client, client):
    auth_check(admin_client.schema, 'configItem', 'r', {
        'sourceVersion': 'r',
    })


def test_config_item_status_auth(admin_client, client):
    auth_check(admin_client.schema, 'configItemStatus', 'ru', {
        'agentId': 'r',
        'appliedUpdated': 'r',
        'appliedVersion': 'ru',
        'requestedUpdated': 'r',
        'requestedVersion': 'r',
        'sourceVersion': 'r',
    })


def test_setting_auth(admin_client, client):
    auth_check(admin_client.schema, 'setting', 'crud', {
        'name': 'cr',
        'value': 'cru',
    })


def git(admin_client, client):
    auth_check(admin_client.schema, 'schema', 'r', {
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

    auth_check(client.schema, 'schema', 'r', {
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


def test_account_auth(admin_client, client):
    auth_check(admin_client.schema, 'account', 'crud', {
        'id': 'cru',
        'externalId': 'cru',
        'externalIdType': 'cru',
        'removeTime': 'ru',
        'data': 'r',
        'kind': 'cr',
        'uuid': 'cr',
    })

    auth_check(client.schema, 'account', 'r', {

    })


def test_agent_auth(admin_client, client):
    auth_check(admin_client.schema, 'agent', 'r', {
        'managedConfig': 'r',
        'uri': 'r',
        'accountId': 'r',
        })

    auth_check(client.schema, 'agent', 'r', {
        'accountId': 'r',
    })


def test_extension_point_auth(admin_client, client):
    auth_check(admin_client.schema, 'extensionPoint', 'r', {
        'excludeSetting': 'r',
        'includeSetting': 'r',
        'listSetting': 'r',
    })


def test_api_key_auth(admin_client, client):
    auth_check(admin_client.schema, 'apiKey', 'crud', {
        'publicValue': 'cr',
        'secretValue': 'cr',
        'removeTime': 'ru',
        'data': 'r',
        'accountId': 'cr',
        'kind': 'cr',
        'uuid': 'r',
    })

    auth_check(client.schema, 'apiKey', 'crud', {
        'publicValue': 'r',
        'accountId': 'r',
    })


def test_subscribe_auth(admin_client, client):
    auth_check(admin_client.schema, 'subscribe', 'cr', {
        'eventNames': 'cr',
        'agentId': 'r',
    })

    auth_check(client.schema, 'subscribe', 'cr', {
        'eventNames': 'cr',
        'agentId': 'r',
    })


def test_registration_tokens_auth(admin_client, client):
    auth_check(admin_client.schema, 'registrationToken', 'cr', {
        'created': 'r',
        'data': 'r',
        'description': 'cr',
        'removeTime': 'r',
        'accountId': 'r',
        'kind': 'cr',
        'uuid': 'r',
        })

    auth_check(client.schema, 'registrationToken', 'cr', {
        'accountId': 'r',
        'created': 'r',
        'description': 'cr',
        'uuid': 'r',
    })


def test_type_documentation_auth(admin_client, client):
    auth_check(admin_client.schema, 'typeDocumentation', 'r', {

    })

    auth_check(client.schema, 'typeDocumentation', 'r', {

    })


def test_stats_access_auth(admin_client, client):
    auth_check(admin_client.schema, 'statsAccess', 'r', {
        'token': 'r',
        'url': 'r',
    })

    auth_check(client.schema, 'statsAccess', 'r', {
        'token': 'r',
        'url': 'r',
    })
