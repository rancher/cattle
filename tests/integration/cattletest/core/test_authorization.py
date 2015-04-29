from common_fixtures import *  # NOQA


def test_client_access(clients):
    typesLen = {
        'admin': 92,
        'agent': 8,
        'user': 70,
        'agentRegister': 4,
        'test': 140,
        'readAdmin': 92,
        'token': 2,
        'superadmin': 142,
        'service': 92,
        'project': 70,
    }
    for tuple in clients.items():
        assert typesLen[tuple[0]] == len(tuple[1].schema.types.items())


def test_instance_link_auth(admin_client, client):
    auth_check(admin_client.schema, 'instanceLink', 'ru', {
        'accountId': 'r',
        'data': 'r',
        'instanceId': 'r',
        'linkName': 'r',
        'ports': 'r',
        'targetInstanceId': 'ru',
    })

    auth_check(client.schema, 'instanceLink', 'ru', {
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
        'accountId': 'r',
        'defaultProject': 'r'
    })


def test_github_auth(admin_client):
    auth_check(admin_client.schema, 'githubconfig', 'cru', {
        'enabled': 'cr',
        'allowedOrganizations': 'cr',
        'allowedUsers': 'cr',
        'clientId': 'cr',
        'clientSecret': 'cr',
        'accessMode': 'cr'
    })


def test_project_auth(admin_client, client):
    auth_check(admin_client.schema, 'project', 'crud', {
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'cr',
        'data': 'r',
        'members': 'cr',
        'projectId': 'r'
    })

    auth_check(client.schema, 'project', 'crud', {
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'r',
        'members': 'cr'
    })


def test_project_member_auth(admin_client, client):
    auth_check(admin_client.schema, 'projectMember', 'r', {
        "role": "r",
        "externalId": "r",
        "externalIdType": "r",
        "projectId": "r",
        "data": 'r'
    })

    auth_check(client.schema, 'projectMember', 'r', {
        "role": "r",
        "externalId": "r",
        "externalIdType": "r",
        "projectId": "r"
    })


def test_host_auth(admin_client, client):
    auth_check(admin_client.schema, 'host', 'rud', {
        'accountId': 'r',
        'apiProxy': 'ru',
        'agentId': 'r',
        'computeTotal': 'r',
        'data': 'r',
        'physicalHostId': 'r',
        'info': 'r',
    })

    auth_check(client.schema, 'host', 'rud', {
        'accountId': 'r',
        'computeTotal': 'r',
        'physicalHostId': 'r',
        'info': 'r',
    })


def test_ip_address_auth(admin_client, client):
    auth_check(admin_client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'networkId': 'r',
        'address': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'ipAddress', 'r', {
        'accountId': 'r',
        'address': 'r',
        'networkId': 'r',
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

    auth_check(client.schema, 'volume', 'rd', {
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


def test_container_auth(admin_client, client):
    auth_check(admin_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'agentId': 'r',
        'allocationState': 'r',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'data': 'r',
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
        'ports': 'cr',
        'primaryIpAddress': 'r',
        'privileged': 'cr',
        'publishAllPorts': 'cr',
        'removeTime': 'r',
        'registryCredentialId': 'cr',
        'requestedHostId': 'cr',
        'restartPolicy': 'cr',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'token': 'r',
        'tty': 'cr',
        'user': 'cr',
        'systemContainer': 'r',
        'nativeContainer': 'r',
        'externalId': 'r'
    })

    auth_check(client.schema, 'container', 'crud', {
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


def test_process_execution(admin_client, client):
    auth_check(admin_client.schema, 'processExecution', 'r', {
        'log': 'r',
        'processInstanceId': 'r',
    })


def test_process_definition(admin_client, client):
    auth_check(admin_client.schema, 'processDefinition', 'r', {
        'extensionBased': 'r',
        'preProcessListeners': 'r',
        'postProcessListeners': 'r',
        'processHandlers': 'r',
        'resourceType': 'r',
        'stateTransitions': 'r',
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
        'id': 'r',
        'externalId': 'cru',
        'externalIdType': 'cru',
        'removeTime': 'r',
        'data': 'r',
        'kind': 'cru',
        'uuid': 'cr',
        'projectId': 'r'
    })

    auth_check(client.schema, 'account', 'r', {
    })


def test_agent_auth(admin_client, client):
    auth_check(admin_client.schema, 'agent', 'r', {
        'managedConfig': 'r',
        'uri': 'r',
        'accountId': 'r',
        'data': 'r',
    })


def test_extension_point_auth(admin_client, client):
    auth_check(admin_client.schema, 'extensionPoint', 'r', {
        'excludeSetting': 'r',
        'includeSetting': 'r',
        'listSetting': 'r',
        'implementations': 'r',
    })


def test_api_key_auth(admin_client, client):
    auth_check(admin_client.schema, 'apiKey', 'crud', {
        'publicValue': 'cr',
        'secretValue': 'cr',
        'removeTime': 'r',
        'data': 'r',
        'accountId': 'cr',
    })

    auth_check(client.schema, 'apiKey', 'crud', {
        'publicValue': 'r',
        'accountId': 'r',
        'secretValue': 'r',
    })


def test_subscribe_auth(admin_client, client):
    auth_check(admin_client.schema, 'subscribe', 'cr', {
        'eventNames': 'cr',
        'agentId': 'cr',
    })

    auth_check(client.schema, 'subscribe', 'cr', {
        'eventNames': 'cr',
    })


def test_registration_tokens_auth(admin_client, client, service_client):
    auth_check(admin_client.schema, 'registrationToken', 'cr', {
        'created': 'r',
        'data': 'r',
        'description': 'cr',
        'removeTime': 'r',
        'accountId': 'r',
    })

    auth_check(service_client.schema, 'registrationToken', 'cr', {
        'created': 'r',
        'data': 'r',
        'description': 'cr',
        'removeTime': 'r',
        'accountId': 'cr',
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


def test_account_resource_auth(admin_client, client):
    resource_action_check(admin_client.schema, 'account', [
        'update',
        'activate',
        'deactivate',
        'restore',
        'remove',
        'purge',
        'create'
    ])


def test_machine(admin_client, client, service_client):
    auth_check(admin_client.schema, 'machine', 'crd', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'data': 'r',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'virtualboxConfig': 'cr',
        'digitaloceanConfig': 'cr',
        'amazonec2Config': 'cr',
        'packetConfig': 'cr',
    })

    auth_check(client.schema, 'machine', 'crd', {
        'driver': 'r',
        'accountId': 'r',
        'externalId': 'r',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'virtualboxConfig': 'cr',
        'digitaloceanConfig': 'cr',
        'amazonec2Config': 'cr',
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
        'packetConfig': 'cr',
    })


def test_physical_host(admin_client, client, service_client):
    auth_check(admin_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
        'data': 'r',

    })

    auth_check(client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
    })


def test_registry_credentials(admin_client, client):
    auth_check(admin_client.schema, 'registryCredential', 'crud', {
        'accountId': 'r',
        'data': 'r',
        'email': 'cru',
        'publicValue': 'cru',
        'secretValue': 'cru',
        'registryId': 'cr',
    })

    auth_check(client.schema, 'registryCredential', 'crud', {
        'accountId': 'r',
        'email': 'cru',
        'publicValue': 'cru',
        'secretValue': 'cru',
        'registryId': 'cr',
    })


def test_registry(admin_client, client):
    auth_check(admin_client.schema, 'registry', 'crud', {
        'accountId': 'r',
        'data': 'r',
        'serverAddress': 'cr',
    })

    auth_check(client.schema, 'registry', 'crud', {
        'accountId': 'r',
        'serverAddress': 'cr',
    })


def test_lb_config_listener_map(admin_client, client):
    auth_check(admin_client.schema, 'loadBalancerConfigListenerMap', 'r', {
        'loadBalancerConfigId': 'r',
        'loadBalancerListenerId': 'r',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'loadBalancerConfigListenerMap', 'r', {
        'loadBalancerConfigId': 'r',
        'loadBalancerListenerId': 'r',
        'accountId': 'r',
    })


def test_lb_host_map(admin_client, client):
    auth_check(admin_client.schema, 'loadBalancerHostMap', 'r', {
        'hostId': 'r',
        'loadBalancerId': 'r',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'loadBalancerHostMap', 'r', {
        'hostId': 'r',
        'loadBalancerId': 'r',
        'accountId': 'r',
        })


def test_container_events(admin_client, client, agent_client):
    auth_check(admin_client.schema, 'containerEvent', 'r', {
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

    auth_check(client.schema, 'containerEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'externalFrom': 'r',
        'reportedHostUuid': 'r',
        'externalId': 'r',
        'externalStatus': 'r',
        'accountId': 'r',
        'dockerInspect': 'r'
    })


def test_svc_discovery_service(admin_client, client):
    auth_check(admin_client.schema, 'service', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'service', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
    })


def test_svc_discovery_environment(admin_client, client):
    auth_check(admin_client.schema, 'environment', 'crud', {
        'name': 'cru',
        'accountId': 'r',
        'data': 'r',
    })

    auth_check(client.schema, 'environment', 'crud', {
        'name': 'cru',
        'accountId': 'r',
    })


def test_svc_discovery_lb_service(admin_client, client):
    auth_check(admin_client.schema, 'loadBalancerService', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
        'data': 'r',
        'loadBalancerConfig': 'cr',
    })

    auth_check(client.schema, 'loadBalancerService', 'crud', {
        'name': 'cru',
        'environmentId': 'cr',
        'scale': 'cru',
        'dataVolumesFromService': 'cr',
        'launchConfig': 'cr',
        'accountId': 'r',
        'loadBalancerConfig': 'cr',
    })
