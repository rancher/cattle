from common_fixtures import *  # NOQA


@pytest.fixture(scope='module')
def user_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='user').user_client


@pytest.fixture(scope='module')
def read_admin_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='readAdmin').user_client


@pytest.fixture(scope='module')
def project_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='project').user_client


@pytest.fixture(scope='module')
def token_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='token').user_client


@pytest.fixture(scope='module')
def agent_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='agent').user_client


@pytest.fixture(scope='module')
def agent_register_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='agentRegister').user_client


@pytest.fixture(scope='module')
def service_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='service').user_client


@pytest.fixture(scope='module')
def project_admin_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='projectadmin').user_client


@pytest.fixture(scope='module')
def owner_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='owner').user_client


@pytest.fixture(scope='module')
def environment_client(admin_user_client):
    return create_context(admin_user_client, create_project=False,
                          add_host=False, kind='environment').user_client


def _clean_types(types):
    for i in ['openstackConfig',
              'ubiquityConfig',
              'packetConfig',
              'notThere',
              'azureConfig',
              'vmwarevcloudairConfig',
              'exoscaleConfig',
              'rackspaceConfig',
              'hypervConfig',
              'googleConfig',
              'vmwarevsphereConfig',
              'virtualboxConfig',
              'amazonec2Config',
              'genericConfig',
              'vmwarefusionConfig',
              'digitaloceanConfig',
              'softlayerConfig',
              'noneConfig']:
        try:
            types.remove(i)
        except ValueError:
            pass
        except KeyError:
            pass
    return types


def test_user_types(user_client, adds=set(), removes=set()):
    types = {
        'account',
        'addOutputsInput',
        'addRemoveServiceLinkInput',
        'apiKey',
        'auditLog',
        'backup',
        'backupTarget',
        'baseMachineConfig',
        'catalogTemplate',
        'certificate',
        'changeSecretInput',
        'composeService',
        'composeProject',
        'composeConfig',
        'composeConfigInput',
        'container',
        'containerEvent',
        'containerExec',
        'containerLogs',
        'containerProxy',
        'credential',
        'dnsService',
        'stack',
        'stackUpgrade',
        'externalService',
        'externalEvent',
        'externalServiceEvent',
        'externalStoragePoolEvent',
        'externalVolumeEvent',
        'externalDnsEvent',
        'externalHostEvent',
        'fieldDocumentation',
        'host',
        'hostAccess',
        'identity',
        'image',
        'instance',
        'instanceConsole',
        'instanceConsoleInput',
        'instanceHealthCheck',
        'instanceLink',
        'instanceStop',
        'ipAddress',
        'kubernetesService',
        'kubernetesStack',
        'kubernetesStackUpgrade',
        'label',
        'loadBalancerService',
        'logConfig',
        'machine',
        'machineDriver',
        'mount',
        'mountEntry',
        'network',
        'networkDriver',
        'networkPolicyRule',
        'networkPolicyRuleBetween',
        'networkPolicyRuleMember',
        'networkPolicyRuleWithin',
        'password',
        'physicalHost',
        'port',
        'project',
        'projectMember',
        'projectTemplate',
        'pullTask',
        'register',
        'registrationToken',
        'registry',
        'registryCredential',
        'restartPolicy',
        'restoreFromBackupInput',
        'revertToSnapshotInput',
        'schema',
        'secret',
        'secretReference',
        'service',
        'serviceExposeMap',
        'serviceLog',
        'serviceProxy',
        'setProjectMembersInput',
        'setServiceLinksInput',
        'snapshot',
        'snapshotBackupInput',
        'statsAccess',
        'storageDriver',
        'storagePool',
        'typeDocumentation',
        'userPreference',
        'volume',
        'launchConfig',
        'serviceEvent',
        'activeSetting',
        'serviceConsumeMap',
        'setting',
        'dockerBuild',
        'secondaryLaunchConfig',
        'serviceLink',
        'serviceUpgrade',
        'serviceUpgradeStrategy',
        'toServiceUpgradeStrategy',
        'inServiceUpgradeStrategy',
        'virtualMachine',
        'virtualMachineDisk',
        'publicEndpoint',
        'serviceRestart',
        'rollingRestartStrategy',
        'servicesPortRange',
        'healthcheckInstanceHostMap',
        'recreateOnQuorumStrategyConfig',
        'volumeSnapshotInput',
        'nfsConfig',
        'blkioDeviceOption',
        'scalePolicy',
        'binding',
        'serviceBinding',
        'volumeTemplate',
        'volumeActivateInput',
        'ulimit',
        'lbConfig',
        'lbTargetConfig',
        'portRule',
        'targetPortRule',
        'loadBalancerCookieStickinessPolicy',
        'subnet',
        'storageDriverService',
        'networkDriverService',
        'defaultNetwork',
        'scheduledUpgrade',
        'genericObject',
        'hostTemplate',
    }
    types.update(adds)
    types.difference_update(removes)
    assert set(_clean_types(user_client.schema.types.keys())) == types
    return types


def test_project_types(project_client, environment_client,
                       project_admin_client):
    # Almost the same as user
    test_user_types(project_client, adds={'subscribe'},
                    removes={'userPreference'})
    test_user_types(environment_client, adds={'subscribe'},
                    removes={'userPreference', 'secret', 'secretReference'})
    test_user_types(project_admin_client, adds={'subscribe'},
                    removes={'userPreference', 'secret', 'secretReference'})


def test_readonly_types(admin_user_client):
    context = create_context(admin_user_client, kind='readonly')
    client = context.user_client
    test_user_types(client, adds={'subscribe'},
                    removes={'userPreference', 'registrationToken'})
    for type in _clean_types(set(client.schema.types.keys())):
        type = client.schema.types[type]
        assert len(type['actions']) == 0
        if type.id == 'container':
            assert type['resourceActions'].keys() == ['logs']
        elif type.id == 'virtualMachine':
            assert type['resourceActions'].keys() == ['logs']
        else:
            print type.id
            assert len(type['resourceActions']) == 0
        assert len(type['collectionActions']) == 0
        if type.resourceFields is not None:
            for k, field in type.resourceFields.items():
                assert field.create is False
                assert field.update is False


def test_agent_register_types(agent_register_client):
    assert set(_clean_types(agent_register_client.schema.types.keys())) == {
        'agent',
        'error',
        'schema',
    }


def test_agent_types(agent_client):
    assert set(_clean_types(agent_client.schema.types.keys())) == {
        'agent',
        'configContent',
        'containerEvent',
        'error',
        'externalEvent',
        'externalHostEvent',
        'externalVolumeEvent',
        'externalServiceEvent',
        'externalStoragePoolEvent',
        'externalDnsEvent',
        'hostApiProxyToken',
        'publish',
        'schema',
        'subscribe',
        'serviceEvent',
        'storagePool',
        'volume',
    }


def test_token_types(token_client):
    assert set(token_client.schema.types.keys()) == {
        'schema',
        'token',
    }


def test_service_types(service_client):
    # Almost the same as admin user
    test_admin_types(service_client, adds={'subscribe', 'dynamicSchema'},
                     removes={'userPreference'})


def test_read_admin_types(read_admin_client):
    # same as admin user
    test_admin_types(read_admin_client)


def test_admin_types(admin_user_client, adds=set(), removes=set()):
    types = {
        'account',
        'activeSetting',
        'addOutputsInput',
        'addRemoveServiceLinkInput',
        'agent',
        'apiKey',
        'auditLog',
        'backup',
        'backupTarget',
        'baseMachineConfig',
        'catalogTemplate',
        'certificate',
        'changeSecretInput',
        'clusterMembership',
        'composeConfig',
        'composeConfigInput',
        'composeProject',
        'composeService',
        'configItem',
        'configItemStatus',
        'container',
        'containerEvent',
        'containerExec',
        'containerLogs',
        'containerProxy',
        'credential',
        'databasechangelog',
        'databasechangeloglock',
        'defaultNetwork',
        'dnsService',
        'stack',
        'stackUpgrade',
        'extensionImplementation',
        'extensionPoint',
        'externalHandler',
        'externalHandlerExternalHandlerProcessMap',
        'externalHandlerProcess',
        'externalHandlerProcessConfig',
        'externalService',
        'externalEvent',
        'externalVolumeEvent',
        'externalServiceEvent',
        'externalStoragePoolEvent',
        'externalDnsEvent',
        'externalHostEvent',
        'fieldDocumentation',
        'azureadconfig',
        'haConfig',
        'haConfigInput',
        'host',
        'hostAccess',
        'hostApiProxyToken',
        'identity',
        'image',
        'instance',
        'instanceConsole',
        'instanceConsoleInput',
        'instanceHealthCheck',
        'instanceLink',
        'instanceStop',
        'ipAddress',
        'kubernetesService',
        'kubernetesStack',
        'kubernetesStackUpgrade',
        'label',
        'ldapconfig',
        'loadBalancerService',
        'localAuthConfig',
        'logConfig',
        'machine',
        'machineDriver',
        'mount',
        'mountEntry',
        'network',
        'networkDriver',
        'networkDriverService',
        'networkPolicyRule',
        'networkPolicyRuleBetween',
        'networkPolicyRuleMember',
        'networkPolicyRuleWithin',
        'openldapconfig',
        'password',
        'physicalHost',
        'port',
        'processDefinition',
        'processExecution',
        'processInstance',
        'project',
        'projectMember',
        'projectTemplate',
        'publish',
        'pullTask',
        'register',
        'registrationToken',
        'registry',
        'registryCredential',
        'resourceDefinition',
        'restartPolicy',
        'restoreFromBackupInput',
        'revertToSnapshotInput',
        'schema',
        'secret',
        'secretReference',
        'service',
        'serviceExposeMap',
        'serviceProxy',
        'serviceUpgrade',
        'setProjectMembersInput',
        'setServiceLinksInput',
        'setting',
        'snapshot',
        'snapshotBackupInput',
        'stateTransition',
        'statsAccess',
        'storageDriver',
        'storageDriverService',
        'storagePool',
        'subnet',
        'task',
        'taskInstance',
        'typeDocumentation',
        'userPreference',
        'virtualMachine',
        'virtualMachineDisk',
        'volume',
        'launchConfig',
        'serviceLog',
        'serviceEvent',
        'serviceConsumeMap',
        'dockerBuild',
        'secondaryLaunchConfig',
        'serviceLink',
        'serviceUpgradeStrategy',
        'toServiceUpgradeStrategy',
        'inServiceUpgradeStrategy',
        'publicEndpoint',
        'serviceRestart',
        'rollingRestartStrategy',
        'servicesPortRange',
        'healthcheckInstanceHostMap',
        'recreateOnQuorumStrategyConfig',
        'volumeSnapshotInput',
        'nfsConfig',
        'blkioDeviceOption',
        'scalePolicy',
        'binding',
        'serviceBinding',
        'volumeTemplate',
        'volumeActivateInput',
        'ulimit',
        'lbConfig',
        'lbTargetConfig',
        'portRule',
        'targetPortRule',
        'loadBalancerCookieStickinessPolicy',
        'genericObject',
        'processPool',
        'processSummary',
        'scheduledUpgrade',
        'hostTemplate',
    }
    types.update(adds)
    types.difference_update(removes)
    assert set(_clean_types(admin_user_client.schema.types.keys())) == types


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
    auth_check(token_client.schema, 'token', 'crd', {
        'jwt': 'r',
        'code': 'cr',
        'user': 'r',
        'security': 'r',
        'userType': 'r',
        'accountId': 'r',
        'userIdentity': 'r',
        'authProvider': 'r',
        'redirectUrl': 'r',
        'enabled': 'r'
    })


def test_azure_auth(admin_user_client, user_client, project_client):
    assert 'azureadconfig' not in user_client.schema.types
    assert 'azureadconfig' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'azureadconfig', 'cru', {
        'enabled': 'cr',
        'clientId': 'cr',
        'tenantId': 'cr',
        'adminAccountPassword': 'cro',
        'adminAccountUsername': 'cr',
        'accessMode': 'cr',
        'domain': 'cr'
    })


def test_ldap_auth(admin_user_client, user_client, project_client):
    assert 'ldapconfig' not in user_client.schema.types
    assert 'ldapconfig' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'ldapconfig', 'cru', {
        'accessMode': 'cr',
        'domain': 'cr',
        'groupSearchDomain': 'cr',
        'enabled': 'cr',
        'groupNameField': 'cr',
        'groupObjectClass': 'cr',
        'groupSearchField': 'cr',
        'loginDomain': 'cr',
        'name': 'r',
        'port': 'cr',
        'server': 'cr',
        'serviceAccountPassword': 'cro',
        'serviceAccountUsername': 'cr',
        'tls': 'cr',
        'userDisabledBitMask': 'cr',
        'userEnabledAttribute': 'cr',
        'userLoginField': 'cr',
        'userNameField': 'cr',
        'userObjectClass': 'cr',
        'userSearchField': 'cr',
        'groupMemberMappingAttribute': 'cr',
        'userMemberAttribute': 'cr',
        'connectionTimeout': 'cr',
        'allowedIdentities': 'cr'
    })


def test_openldap_auth(admin_user_client, user_client, project_client):
    assert 'openldapconfig' not in user_client.schema.types
    assert 'openldapconfig' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'openldapconfig', 'cru', {
        'accessMode': 'cr',
        'domain': 'cr',
        'groupSearchDomain': 'cr',
        'enabled': 'cr',
        'groupNameField': 'cr',
        'groupObjectClass': 'cr',
        'groupSearchField': 'cr',
        'loginDomain': 'cr',
        'name': 'r',
        'port': 'cr',
        'server': 'cr',
        'serviceAccountPassword': 'cro',
        'serviceAccountUsername': 'cr',
        'tls': 'cr',
        'userDisabledBitMask': 'cr',
        'userEnabledAttribute': 'cr',
        'userLoginField': 'cr',
        'userNameField': 'cr',
        'userObjectClass': 'cr',
        'userSearchField': 'cr',
        'groupMemberMappingAttribute': 'cr',
        'userMemberAttribute': 'cr',
        'connectionTimeout': 'cr',
        'groupDNField': 'cr',
        'groupMemberUserAttribute': 'cr',
        'allowedIdentities': 'cr'
    })


def test_audit_logs(admin_user_client, user_client, project_client):
    assert 'auditLog' in user_client.schema.types
    assert 'auditLog' in project_client.schema.types

    auth_check(user_client.schema, 'auditLog', 'r', {
        'accountId': 'r',
        'authenticatedAsAccountId': 'r',
        'authenticatedAsIdentityId': 'r',
        'authType': 'r',
        'created': 'r',
        'description': 'r',
        'eventType': 'r',
        'requestObject': 'r',
        'resourceId': 'r',
        'resourceType': 'r',
        'responseCode': 'r',
        'responseObject': 'r',
        'clientIp': 'r'
    })


def test_local_auth(admin_user_client, user_client, project_client):
    assert 'localauthconfig' not in user_client.schema.types
    assert 'localauthconfig' not in project_client.schema.types

    auth_check(admin_user_client.schema, 'localAuthConfig', 'cr', {
        'accessMode': 'cr',
        'name': 'cr',
        'username': 'cr',
        'password': 'cro',
        'enabled': 'cr',
    })


def test_project_auth(admin_user_client, user_client, service_client,
                      project_client):
    auth_check(admin_user_client.schema, 'project', 'crud', {
        'allowSystemRole': 'cru',
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'cr',
        'data': 'r',
        'members': 'cr',
        'virtualMachine': 'cru',
        'servicesPortRange': 'cru',
        'healthState': "r",
        'projectTemplateId': 'cr',
        'orchestration': 'r',
        'version': 'r',
        'defaultNetworkId': 'r',
        'projectLinks': 'cru',
        'hostRemoveDelaySeconds': 'cru'
    })

    auth_check(user_client.schema, 'project', 'crud', {
        'description': 'cru',
        'kind': 'r',
        'name': 'cru',
        'uuid': 'r',
        'members': 'cr',
        'virtualMachine': 'cru',
        'servicesPortRange': 'cru',
        'healthState': "r",
        'projectTemplateId': 'cr',
        'orchestration': 'r',
        'version': 'r',
        'defaultNetworkId': 'r',
        'hostRemoveDelaySeconds': 'cru'
    })

    auth_check(project_client.schema, 'project', 'r', {
        'description': 'r',
        'kind': 'r',
        'name': 'r',
        'uuid': 'r',
        'members': 'r',
        'virtualMachine': 'r',
        'servicesPortRange': 'r',
        'healthState': "r",
        'projectTemplateId': 'r',
        'orchestration': 'r',
        'version': 'r',
        'defaultNetworkId': 'r',
        'hostRemoveDelaySeconds': 'r'
    })

    auth_check(service_client.schema, 'project', 'cr', {
        'allowSystemRole': 'cr',
        'data': 'r',
        'virtualMachine': 'cr',
        'members': 'cr',
        'servicesPortRange': 'cr',
        'uuid': 'cr',
        'healthState': 'r',
        'projectTemplateId': 'cr',
        'orchestration': 'r',
        'version': 'r',
        'defaultNetworkId': 'r',
        'projectLinks': 'cr',
        'hostRemoveDelaySeconds': 'cr'
    })


def test_project_member_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'projectMember', 'cr', {
        'name': 'r',
        'role': 'cr',
        'externalId': 'cr',
        'externalIdType': 'cr',
        'projectId': 'r',
        'data': 'r'
    })

    auth_check(user_client.schema, 'projectMember', 'cr', {
        'name': 'r',
        'role': 'cr',
        'externalId': 'cr',
        'externalIdType': 'cr',
        'projectId': 'r',
    })

    auth_check(project_client.schema, 'projectMember', 'r', {
        'name': 'r',
        'role': 'r',
        'externalId': 'r',
        'externalIdType': 'r',
        'projectId': 'r',
    })


def test_host_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'host', 'r', {
        'accountId': 'r',
        'agentState': 'r',
        'apiProxy': 'r',
        'agentId': 'r',
        'computeTotal': 'r',
        'data': 'r',
        'physicalHostId': 'r',
        'hostname': 'r',
        'info': 'r',
        'labels': 'r',
        'publicEndpoints': 'r',
        'agentIpAddress': 'r',
        'instanceIds': 'r',
        'localStorageMb': 'r',
        'memory': 'r',
        'milliCpu': 'r',
        'stackId': 'r',
        'hostTemplateId': 'r',
    })

    auth_check(user_client.schema, 'host', 'r', {
        'accountId': 'r',
        'agentState': 'r',
        'computeTotal': 'r',
        'physicalHostId': 'r',
        'hostname': 'r',
        'info': 'r',
        'labels': 'r',
        'agentIpAddress': 'r',
        'publicEndpoints': 'r',
        'instanceIds': 'r',
        'localStorageMb': 'r',
        'memory': 'r',
        'milliCpu': 'r',
        'stackId': 'r',
        'hostTemplateId': 'r',
    })

    auth_check(project_client.schema, 'host', 'rud', {
        'accountId': 'r',
        'agentState': 'r',
        'computeTotal': 'r',
        'physicalHostId': 'r',
        'hostname': 'r',
        'info': 'r',
        'labels': 'ru',
        'agentIpAddress': 'r',
        'publicEndpoints': 'r',
        'instanceIds': 'r',
        'localStorageMb': 'ru',
        'memory': 'ru',
        'milliCpu': 'ru',
        'stackId': 'r',
        'hostTemplateId': 'r',
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


def test_storagepool_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'storagePool', 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'name': 'r',
        'driverName': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'storageDriverId': 'r',
        'hostIds': 'r',
        'volumeIds': 'r',
    })

    auth_check(user_client.schema, 'storagePool', 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'name': 'r',
        'driverName': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'storageDriverId': 'r',
        'hostIds': 'r',
        'volumeIds': 'r',
    })

    auth_check(project_client.schema, 'storagePool', 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'name': 'r',
        'driverName': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'storageDriverId': 'r',
        'hostIds': 'r',
        'volumeIds': 'r',
    })


def test_volume_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'volume', 'r', {
        'accountId': 'r',
        'created': 'r',
        'data': 'r',
        'description': 'r',
        'externalId': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'mounts': 'r',
        'name': 'r',
        'removeTime': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'driver': 'r',
        'driverOpts': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r',
        'accessMode': 'r',
        'stackId': 'r',
        'volumeTemplateId': 'r',
        'storageDriverId': 'r',
        'hostId': 'r',
        'sizeMb': 'r',
    })

    auth_check(user_client.schema, 'volume', 'r', {
        'accountId': 'r',
        'created': 'r',
        'description': 'r',
        'externalId': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'kind': 'r',
        'mounts': 'r',
        'name': 'r',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'driver': 'r',
        'driverOpts': 'r',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r',
        'accessMode': 'r',
        'stackId': 'r',
        'volumeTemplateId': 'r',
        'storageDriverId': 'r',
        'hostId': 'r',
        'sizeMb': 'r',
    })

    auth_check(project_client.schema, 'volume', 'crud', {
        'accountId': 'r',
        'created': 'r',
        'description': 'cru',
        'externalId': 'r',
        'id': 'r',
        'imageId': 'r',
        'instanceId': 'r',
        'mounts': 'r',
        'kind': 'r',
        'name': 'cr',
        'removed': 'r',
        'state': 'r',
        'uri': 'r',
        'uuid': 'r',
        'driver': 'cr',
        'driverOpts': 'cru',
        'transitioning': 'r',
        'transitioningMessage': 'r',
        'transitioningProgress': 'r',
        'isHostPath': 'r',
        'accessMode': 'r',
        'stackId': 'cr',
        'volumeTemplateId': 'cr',
        'storageDriverId': 'cr',
        'hostId': 'cru',
        'sizeMb': 'cr',
    })


def test_container_auth(admin_user_client, user_client, project_client,
                        environment_client, project_admin_client):
    auth_check(admin_user_client.schema, 'container', 'r', {
        'accountId': 'r',
        'agentId': 'r',
        'allocationState': 'r',
        'blkioDeviceOptions': 'r',
        'build': 'r',
        'capAdd': 'r',
        'capDrop': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'data': 'r',
        'dataVolumes': 'r',
        'dataVolumeMounts': 'r',
        'dataVolumesFrom': 'r',
        'description': 'r',
        'devices': 'r',
        'workingDir': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'entryPoint': 'r',
        'environment': 'r',
        'firstRunning': 'r',
        'hostname': 'r',
        'hostId': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'lxcConf': 'r',
        'mounts': 'r',
        'memory': 'r',
        'memoryReservation': 'r',
        'memorySwap': 'r',
        'milliCpuReservation': 'r',
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
        'serviceIds': 'r',
        'stdinOpen': 'r',
        'token': 'r',
        'tty': 'r',
        'user': 'r',
        'userPorts': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r',
        'healthCheck': 'r',
        'healthState': 'r',
        'securityOpt': 'r',
        'logConfig': 'r',
        'pidMode': 'r',
        'volumeDriver': 'r',
        'extraHosts': 'r',
        'readOnly': 'r',
        'expose': 'r',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'startCount': 'r',
        'system': 'r',
        'blkioWeight': 'r',
        'cgroupParent': 'r',
        'usernsMode': 'r',
        'pidsLimit': 'r',
        'diskQuota': 'r',
        'cpuCount': 'r',
        'cpuPercent': 'r',
        'ioMaximumIOps': 'r',
        'ioMaximumBandwidth': 'r',
        'cpuPeriod': 'r',
        'cpuQuota': 'r',
        'cpuSetMems': 'r',
        'dnsOpt': 'r',
        'groupAdd': 'r',
        'isolation': 'r',
        'kernelMemory': 'r',
        'memorySwappiness': 'r',
        'oomKillDisable': 'r',
        'shmSize': 'r',
        'uts': 'r',
        'ipcMode': 'r',
        'stopSignal': 'r',
        'sysctls': 'r',
        'storageOpt': 'r',
        'oomScoreAdj': 'r',
        'ulimits': 'r',
        'tmpfs': 'r',
        'ip': 'r',
        'ip6': 'r',
        'netAlias': 'r',
        'healthCmd': 'r',
        'healthInterval': 'r',
        'healthTimeout': 'r',
        'healthRetries': 'r',
        'instanceTriggeredStop': 'r',
        'primaryNetworkId': 'r',
        'secrets': 'r',
        'serviceId': 'r',
        'stackId': 'r',
    })

    auth_check(user_client.schema, 'container', 'r', {
        'accountId': 'r',
        'blkioDeviceOptions': 'r',
        'build': 'r',
        'capAdd': 'r',
        'capDrop': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'dataVolumes': 'r',
        'dataVolumeMounts': 'r',
        'dataVolumesFrom': 'r',
        'description': 'r',
        'devices': 'r',
        'workingDir': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'entryPoint': 'r',
        'environment': 'r',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'lxcConf': 'r',
        'mounts': 'r',
        'memory': 'r',
        'memoryReservation': 'r',
        'memorySwap': 'r',
        'milliCpuReservation': 'r',
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
        'serviceIds': 'r',
        'startOnCreate': 'r',
        'stdinOpen': 'r',
        'tty': 'r',
        'user': 'r',
        'userPorts': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r',
        'healthCheck': 'r',
        'healthState': 'r',
        'securityOpt': 'r',
        'logConfig': 'r',
        'pidMode': 'r',
        'extraHosts': 'r',
        'volumeDriver': 'r',
        'readOnly': 'r',
        'expose': 'r',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'system': 'r',
        'startCount': 'r',
        'blkioWeight': 'r',
        'cgroupParent': 'r',
        'usernsMode': 'r',
        'pidsLimit': 'r',
        'diskQuota': 'r',
        'cpuCount': 'r',
        'cpuPercent': 'r',
        'ioMaximumIOps': 'r',
        'ioMaximumBandwidth': 'r',
        'cpuPeriod': 'r',
        'cpuQuota': 'r',
        'cpuSetMems': 'r',
        'dnsOpt': 'r',
        'groupAdd': 'r',
        'isolation': 'r',
        'kernelMemory': 'r',
        'memorySwappiness': 'r',
        'oomKillDisable': 'r',
        'shmSize': 'r',
        'uts': 'r',
        'ipcMode': 'r',
        'stopSignal': 'r',
        'sysctls': 'r',
        'storageOpt': 'r',
        'oomScoreAdj': 'r',
        'ulimits': 'r',
        'tmpfs': 'r',
        'ip': 'r',
        'ip6': 'r',
        'netAlias': 'r',
        'healthCmd': 'r',
        'healthInterval': 'r',
        'healthTimeout': 'r',
        'healthRetries': 'r',
        'instanceTriggeredStop': 'r',
        'primaryNetworkId': 'r',
        'secrets': 'r',
        'serviceId': 'r',
        'stackId': 'r',
    })

    auth_check(project_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'blkioDeviceOptions': 'cr',
        'build': 'cr',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'dataVolumeMounts': 'cr',
        'description': 'cru',
        'devices': 'cr',
        'workingDir': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domainName': 'cr',
        'entryPoint': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'lxcConf': 'cr',
        'mounts': 'r',
        'memory': 'cr',
        'memoryReservation': 'cr',
        'memorySwap': 'cr',
        'milliCpuReservation': 'cr',
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
        'serviceIds': 'r',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'tty': 'cr',
        'user': 'cr',
        'userPorts': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'cr',
        'healthCheck': 'cr',
        'healthState': 'r',
        'securityOpt': 'cr',
        'logConfig': 'cr',
        'pidMode': 'cr',
        'extraHosts': 'cr',
        'volumeDriver': 'cr',
        'readOnly': 'cr',
        'expose': 'cr',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'system': 'r',
        'startCount': 'r',
        'blkioWeight': 'cr',
        'cgroupParent': 'cr',
        'usernsMode': 'cr',
        'pidsLimit': 'cr',
        'diskQuota': 'cr',
        'cpuCount': 'cr',
        'cpuPercent': 'cr',
        'ioMaximumIOps': 'cr',
        'ioMaximumBandwidth': 'cr',
        'cpuPeriod': 'cr',
        'cpuQuota': 'cr',
        'cpuSetMems': 'cr',
        'dnsOpt': 'cr',
        'groupAdd': 'cr',
        'isolation': 'cr',
        'kernelMemory': 'cr',
        'memorySwappiness': 'cr',
        'oomKillDisable': 'cr',
        'shmSize': 'cr',
        'uts': 'cr',
        'ipcMode': 'cr',
        'stopSignal': 'cr',
        'sysctls': 'cr',
        'storageOpt': 'cr',
        'oomScoreAdj': 'cr',
        'ulimits': 'cr',
        'tmpfs': 'cr',
        'ip': 'cr',
        'ip6': 'cr',
        'netAlias': 'cr',
        'healthCmd': 'cr',
        'healthInterval': 'cr',
        'healthTimeout': 'cr',
        'healthRetries': 'cr',
        'instanceTriggeredStop': 'cr',
        'primaryNetworkId': 'r',
        'secrets': 'cr',
        'serviceId': 'r',
        'stackId': 'cr',
    })

    auth_check(environment_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'blkioDeviceOptions': 'cr',
        'build': 'cr',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'dataVolumeMounts': 'cr',
        'description': 'cru',
        'devices': 'cr',
        'workingDir': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domainName': 'cr',
        'entryPoint': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'lxcConf': 'cr',
        'mounts': 'r',
        'memory': 'cr',
        'memoryReservation': 'cr',
        'memorySwap': 'cr',
        'milliCpuReservation': 'cr',
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
        'serviceIds': 'r',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'tty': 'cr',
        'user': 'cr',
        'userPorts': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'cr',
        'healthCheck': 'cr',
        'healthState': 'r',
        'securityOpt': 'cr',
        'logConfig': 'cr',
        'pidMode': 'cr',
        'extraHosts': 'cr',
        'volumeDriver': 'cr',
        'readOnly': 'cr',
        'expose': 'cr',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'system': 'r',
        'startCount': 'r',
        'blkioWeight': 'cr',
        'cgroupParent': 'cr',
        'usernsMode': 'cr',
        'pidsLimit': 'cr',
        'diskQuota': 'cr',
        'cpuCount': 'cr',
        'cpuPercent': 'cr',
        'ioMaximumIOps': 'cr',
        'ioMaximumBandwidth': 'cr',
        'cpuPeriod': 'cr',
        'cpuQuota': 'cr',
        'cpuSetMems': 'cr',
        'dnsOpt': 'cr',
        'groupAdd': 'cr',
        'isolation': 'cr',
        'kernelMemory': 'cr',
        'memorySwappiness': 'cr',
        'oomKillDisable': 'cr',
        'shmSize': 'cr',
        'uts': 'cr',
        'ipcMode': 'cr',
        'stopSignal': 'cr',
        'sysctls': 'cr',
        'storageOpt': 'cr',
        'oomScoreAdj': 'cr',
        'ulimits': 'cr',
        'tmpfs': 'cr',
        'ip': 'cr',
        'ip6': 'cr',
        'netAlias': 'cr',
        'healthCmd': 'cr',
        'healthInterval': 'cr',
        'healthTimeout': 'cr',
        'healthRetries': 'cr',
        'instanceTriggeredStop': 'cr',
        'primaryNetworkId': 'r',
        'serviceId': 'r',
        'stackId': 'cr',
    })

    auth_check(project_admin_client.schema, 'container', 'crud', {
        'accountId': 'r',
        'blkioDeviceOptions': 'cr',
        'build': 'cr',
        'capAdd': 'cr',
        'capDrop': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'dataVolumes': 'cr',
        'dataVolumesFrom': 'cr',
        'dataVolumeMounts': 'cr',
        'description': 'cru',
        'devices': 'cr',
        'workingDir': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domainName': 'cr',
        'entryPoint': 'cr',
        'environment': 'cr',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'lxcConf': 'cr',
        'mounts': 'r',
        'memory': 'cr',
        'memoryReservation': 'cr',
        'memorySwap': 'cr',
        'milliCpuReservation': 'cr',
        'networkIds': 'cr',
        'networkMode': 'cr',
        'networkContainerId': 'cr',
        'ports': 'cr',
        'userPorts': 'r',
        'primaryIpAddress': 'r',
        'privileged': 'cr',
        'publishAllPorts': 'cr',
        'registryCredentialId': 'cr',
        'requestedHostId': 'cr',
        'restartPolicy': 'cr',
        'serviceIds': 'r',
        'startOnCreate': 'cr',
        'stdinOpen': 'cr',
        'tty': 'cr',
        'user': 'cr',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'cr',
        'healthCheck': 'cr',
        'healthState': 'r',
        'securityOpt': 'cr',
        'logConfig': 'cr',
        'pidMode': 'cr',
        'extraHosts': 'cr',
        'volumeDriver': 'cr',
        'readOnly': 'cr',
        'expose': 'cr',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'system': 'r',
        'startCount': 'r',
        'blkioWeight': 'cr',
        'cgroupParent': 'cr',
        'usernsMode': 'cr',
        'pidsLimit': 'cr',
        'diskQuota': 'cr',
        'cpuCount': 'cr',
        'cpuPercent': 'cr',
        'ioMaximumIOps': 'cr',
        'ioMaximumBandwidth': 'cr',
        'cpuPeriod': 'cr',
        'cpuQuota': 'cr',
        'cpuSetMems': 'cr',
        'dnsOpt': 'cr',
        'groupAdd': 'cr',
        'isolation': 'cr',
        'kernelMemory': 'cr',
        'memorySwappiness': 'cr',
        'oomKillDisable': 'cr',
        'shmSize': 'cr',
        'uts': 'cr',
        'ipcMode': 'cr',
        'stopSignal': 'cr',
        'sysctls': 'cr',
        'storageOpt': 'cr',
        'oomScoreAdj': 'cr',
        'ulimits': 'cr',
        'tmpfs': 'cr',
        'ip': 'cr',
        'ip6': 'cr',
        'netAlias': 'cr',
        'healthCmd': 'cr',
        'healthInterval': 'cr',
        'healthTimeout': 'cr',
        'healthRetries': 'cr',
        'instanceTriggeredStop': 'cr',
        'primaryNetworkId': 'r',
        'serviceId': 'r',
        'stackId': 'cr',
    })

    auth_check(project_client.schema, 'dockerBuild', 'cr', {
        'dockerfile': 'cr',
        'context': 'cr',
        'remote': 'cr',
        'nocache': 'cr',
        'rm': 'cr',
        'forcerm': 'cr',
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
        'bindAddress': 'r',
    })

    auth_check(user_client.schema, 'port', 'r', {
        'accountId': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'r',
        'bindAddress': 'r',
    })

    auth_check(project_client.schema, 'port', 'ru', {
        'accountId': 'r',
        'instanceId': 'r',
        'privateIpAddressId': 'r',
        'privatePort': 'r',
        'protocol': 'r',
        'publicIpAddressId': 'r',
        'publicPort': 'ru',
        'bindAddress': 'r',
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
        'executionCount': 'r',
        'runAfter': 'r',
        'startTime': 'r',
        'data': 'r',
        'accountId': 'r',
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
        'accountId': 'r',
        'appliedUpdated': 'r',
        'appliedVersion': 'ru',
        'requestedUpdated': 'r',
        'requestedVersion': 'r',
        'sourceVersion': 'r',
    })


def test_setting_auth(admin_user_client, user_client, project_client):
    auth_check(user_client.schema, 'activeSetting', 'r', {
        'name': 'r',
        'activeValue': 'r',
        'value': 'r',
    })

    auth_check(project_client.schema, 'activeSetting', 'r', {
        'name': 'r',
        'activeValue': 'r',
        'value': 'r',
    })

    auth_check(admin_user_client.schema, 'activeSetting', 'rud', {
        'name': 'r',
        'activeValue': 'r',
        'value': 'ru',
        'source': 'r',
        'inDb': 'r',
    })

    auth_check(user_client.schema, 'setting', 'r', {
        'name': 'r',
        'activeValue': 'r',
        'value': 'r',
    })

    auth_check(project_client.schema, 'setting', 'r', {
        'name': 'r',
        'activeValue': 'r',
        'value': 'r',
    })

    auth_check(admin_user_client.schema, 'setting', 'crud', {
        'name': 'cr',
        'activeValue': 'r',
        'value': 'cru',
        'source': 'r',
        'inDb': 'r',
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
        'identity': 'r',
        'removeTime': 'r',
        'data': 'r',
        'kind': 'cru',
        'version': 'r',
        'uuid': 'cr',
    })

    auth_check(user_client.schema, 'account', 'r', {
        'version': 'r',
    })

    auth_check(project_client.schema, 'account', 'r', {
        'version': 'r',
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
        'secretValue': 'cro',
        'removeTime': 'r',
        'data': 'r',
        'accountId': 'cr',
    })

    auth_check(user_client.schema, 'apiKey', 'crud', {
        'publicValue': 'r',
        'accountId': 'r',
        'secretValue': 'ro',
    })

    auth_check(project_client.schema, 'apiKey', 'crud', {
        'publicValue': 'r',
        'accountId': 'r',
        'secretValue': 'ro',
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
        'resourceFields': 'r',
    })

    auth_check(user_client.schema, 'typeDocumentation', 'r', {
        'resourceFields': 'r',
    })

    auth_check(project_client.schema, 'typeDocumentation', 'r', {
        'resourceFields': 'r',
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
        'create',
        'upgrade'
    ])


def test_physical_host(admin_user_client, user_client, service_client,
                       project_client):
    auth_check(admin_user_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
        'data': 'r',
        'driver': 'r',
        'externalId': 'r',
    })

    auth_check(user_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
        'driver': 'r',
        'externalId': 'r',
    })

    auth_check(project_client.schema, 'physicalHost', 'r', {
        'accountId': 'r',
        'driver': 'r',
        'externalId': 'r',
    })


def test_registry_credentials(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'registryCredential', 'r', {
        'accountId': 'r',
        'data': 'r',
        'publicValue': 'r',
        'secretValue': 'ro',
        'registryId': 'r',
    })

    auth_check(user_client.schema, 'registryCredential', 'r', {
        'accountId': 'r',
        'publicValue': 'r',
        'secretValue': 'ro',
        'registryId': 'r',
    })

    auth_check(project_client.schema, 'registryCredential', 'crud', {
        'accountId': 'r',
        'publicValue': 'cru',
        'secretValue': 'curo',
        'registryId': 'cr',
    })


def test_registry(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'registry', 'r', {
        'accountId': 'r',
        'data': 'r',
        'driverName': 'r',
        'externalId': 'r',
        'serverAddress': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
    })

    auth_check(user_client.schema, 'registry', 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'driverName': 'r',
        'serverAddress': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
    })

    auth_check(project_client.schema, 'registry', 'crud', {
        'accountId': 'r',
        'driverName': 'r',
        'externalId': 'r',
        'serverAddress': 'cr',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
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


def test_service_events(admin_user_client, user_client, agent_client,
                        project_client):
    auth_check(admin_user_client.schema, 'serviceEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'instanceId': 'r',
        'accountId': 'r',
        'healthcheckUuid': 'r',
        'reportedHealth': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'serviceEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'instanceId': 'r',
        'accountId': 'r',
        'healthcheckUuid': 'r',
        'reportedHealth': 'r',
    })

    auth_check(project_client.schema, 'serviceEvent', 'r', {
        'externalTimestamp': 'r',
        'hostId': 'r',
        'instanceId': 'r',
        'accountId': 'r',
        'healthcheckUuid': 'r',
        'reportedHealth': 'r',
    })

    auth_check(agent_client.schema, 'serviceEvent', 'cr', {
        'externalTimestamp': 'cr',
        'healthcheckUuid': 'cr',
        'reportedHealth': 'cr',
    })


def test_svc_discovery_service(admin_user_client, user_client, project_client,
                               project_admin_client):
    auth_check(admin_user_client.schema, 'service', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'scale': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'data': 'r',
        'upgrade': 'r',
        'secondaryLaunchConfigs': 'r',
        'vip': 'r',
        'createIndex': 'r',
        'metadata': 'r',
        'selectorLink': 'r',
        'selectorContainer': 'r',
        'fqdn': 'r',
        'publicEndpoints': 'r',
        'retainIp': 'r',
        'assignServiceIpAddress': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'r',
    })

    auth_check(user_client.schema, 'service', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'scale': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'upgrade': 'r',
        'secondaryLaunchConfigs': 'r',
        'vip': 'r',
        'createIndex': 'r',
        'metadata': 'r',
        'selectorLink': 'r',
        'selectorContainer': 'r',
        'fqdn': 'r',
        'publicEndpoints': 'r',
        'retainIp': 'r',
        'assignServiceIpAddress': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'r',
    })

    auth_check(project_client.schema, 'service', 'crud', {
        'name': 'cru',
        'externalId': 'cr',
        'stackId': 'cr',
        'scale': 'cru',
        'launchConfig': 'cr',
        'accountId': 'r',
        'upgrade': 'r',
        'secondaryLaunchConfigs': 'cr',
        'vip': 'cr',
        'createIndex': 'r',
        'metadata': 'cru',
        'selectorLink': 'cru',
        'selectorContainer': 'cru',
        'fqdn': 'r',
        'publicEndpoints': 'cru',
        'retainIp': 'cru',
        'assignServiceIpAddress': 'cr',
        'healthState': 'r',
        'startOnCreate': 'cr',
        'scalePolicy': 'cru',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'cru',
    })

    resource_action_check(user_client.schema, 'service', [
        'activate',
        'addservicelink',
        'cancelupgrade',
        'create',
        'deactivate',
        'finishupgrade',
        'continueupgrade',
        'remove',
        'removeservicelink',
        'restart',
        'rollback',
        'setservicelinks',
        'update',
        'upgrade',
    ])

    resource_action_check(admin_user_client.schema, 'service', [
        'activate',
        'addservicelink',
        'cancelupgrade',
        'create',
        'deactivate',
        'finishupgrade',
        'continueupgrade',
        'remove',
        'removeservicelink',
        'restart',
        'rollback',
        'setservicelinks',
        'update',
        'upgrade',
    ])

    resource_action_check(project_client.schema, 'service', [
        'activate',
        'addservicelink',
        'cancelupgrade',
        'continueupgrade',
        'create',
        'deactivate',
        'finishupgrade',
        'remove',
        'removeservicelink',
        'restart',
        'rollback',
        'setservicelinks',
        'update',
        'upgrade',
    ])

    resource_action_check(project_admin_client.schema, 'service', [
        'activate',
        'addservicelink',
        'cancelupgrade',
        'certificate',
        'create',
        'deactivate',
        'finishupgrade',
        'continueupgrade',
        'remove',
        'removeservicelink',
        'restart',
        'rollback',
        'setservicelinks',
        'update',
        'upgrade',
    ])


def test_auth_compose_project(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'composeProject', 'r', {
        'name': 'r',
        'accountId': 'r',
        'data': 'r',
        'templates': 'r',
        'environment': 'r',
        'answers': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'healthState': 'r',
        'binding': 'r',
        'system': 'r',
        'group': 'r',
        'serviceIds': 'r',
    })

    auth_check(user_client.schema, 'composeProject', 'r', {
        'name': 'r',
        'accountId': 'r',
        'templates': 'r',
        'environment': 'r',
        'answers': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'healthState': 'r',
        'binding': 'r',
        'system': 'r',
        'group': 'r',
        'serviceIds': 'r',
    })

    auth_check(project_client.schema, 'composeProject', 'crud', {
        'name': 'cr',
        'accountId': 'r',
        'templates': 'cr',
        'answers': 'cr',
        'environment': 'cr',
        'externalId': 'cru',
        'previousExternalId': 'cru',
        'previousEnvironment': 'cru',
        'healthState': 'r',
        'binding': 'cru',
        'group': 'cru',
        'system': 'r',
        'serviceIds': 'r',
    })


def test_auth_kubernetes_stack(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'kubernetesStack', 'r', {
        'name': 'r',
        'accountId': 'r',
        'data': 'r',
        'namespace': 'r',
        'templates': 'r',
        'answers': 'r',
        'environment': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'healthState': 'r',
        'binding': 'r',
        'system': 'r',
        'group': 'r',
        'serviceIds': 'r',
    })

    auth_check(user_client.schema, 'kubernetesStack', 'r', {
        'name': 'r',
        'accountId': 'r',
        'namespace': 'r',
        'templates': 'r',
        'answers': 'r',
        'environment': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'healthState': 'r',
        'binding': 'r',
        'system': 'r',
        'group': 'r',
        'serviceIds': 'r',
    })

    auth_check(project_client.schema, 'kubernetesStack', 'crud', {
        'name': 'cr',
        'accountId': 'r',
        'namespace': 'cr',
        'templates': 'cr',
        'answers': 'cr',
        'environment': 'cr',
        'externalId': 'cru',
        'previousExternalId': 'cru',
        'previousEnvironment': 'cru',
        'healthState': 'r',
        'binding': 'cru',
        'system': 'r',
        'group': 'cru',
        'serviceIds': 'r',
    })


def test_svc_discovery_stack(admin_user_client, user_client, project_client,
                             owner_client):
    auth_check(admin_user_client.schema, 'stack', 'r', {
        'name': 'r',
        'accountId': 'r',
        'data': 'r',
        'dockerCompose': 'r',
        'rancherCompose': 'r',
        'environment': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'outputs': 'r',
        'startOnCreate': 'r',
        'healthState': 'r',
        'binding': 'r',
        'group': 'r',
        'system': 'r',
        'serviceIds': 'r',
        'answers': 'r',
        'templates': 'r',
    })

    auth_check(user_client.schema, 'stack', 'r', {
        'name': 'r',
        'accountId': 'r',
        'dockerCompose': 'r',
        'rancherCompose': 'r',
        'environment': 'r',
        'externalId': 'r',
        'previousExternalId': 'r',
        'previousEnvironment': 'r',
        'outputs': 'r',
        'startOnCreate': 'r',
        'healthState': 'r',
        'binding': 'r',
        'group': 'r',
        'system': 'r',
        'serviceIds': 'r',
        'answers': 'r',
        'templates': 'r',
    })

    auth_check(project_client.schema, 'stack', 'crud', {
        'name': 'cru',
        'accountId': 'r',
        'dockerCompose': 'cr',
        'rancherCompose': 'cr',
        'environment': 'cr',
        'externalId': 'cru',
        'previousExternalId': 'cru',
        'previousEnvironment': 'cru',
        'outputs': 'cru',
        'startOnCreate': 'cr',
        'healthState': 'r',
        'binding': 'cru',
        'group': 'cru',
        'system': 'r',
        'serviceIds': 'r',
        'answers': 'cr',
        'templates': 'cr',
    })

    auth_check(owner_client.schema, 'stack', 'crud', {
        'name': 'cru',
        'accountId': 'r',
        'dockerCompose': 'cr',
        'rancherCompose': 'cr',
        'environment': 'cr',
        'externalId': 'cru',
        'previousExternalId': 'cru',
        'previousEnvironment': 'cru',
        'outputs': 'cru',
        'startOnCreate': 'cr',
        'healthState': 'r',
        'binding': 'cru',
        'group': 'cru',
        'system': 'cru',
        'serviceIds': 'r',
        'answers': 'cr',
        'templates': 'cr',
    })


def test_svc_discovery_lb_service(admin_user_client, user_client,
                                  project_client):
    auth_check(admin_user_client.schema, 'loadBalancerService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'scale': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'data': 'r',
        'upgrade': 'r',
        'vip': 'r',
        'metadata': 'r',
        'selectorLink': 'r',
        'fqdn': 'r',
        'publicEndpoints': 'r',
        'retainIp': 'r',
        'assignServiceIpAddress': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'r',
    })

    auth_check(user_client.schema, 'loadBalancerService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'scale': 'r',
        'launchConfig': 'r',
        'accountId': 'r',
        'upgrade': 'r',
        'vip': 'r',
        'metadata': 'r',
        'selectorLink': 'r',
        'fqdn': 'r',
        'publicEndpoints': 'r',
        'retainIp': 'r',
        'assignServiceIpAddress': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'r',
    })

    auth_check(project_client.schema, 'loadBalancerService', 'crud', {
        'name': 'cru',
        'externalId': 'cr',
        'stackId': 'cr',
        'scale': 'cru',
        'launchConfig': 'cru',
        'accountId': 'r',
        'upgrade': 'r',
        'vip': 'cr',
        'metadata': 'cru',
        'selectorLink': 'cru',
        'fqdn': 'r',
        'publicEndpoints': 'cru',
        'retainIp': 'cru',
        'assignServiceIpAddress': 'cr',
        'healthState': 'r',
        'startOnCreate': 'cr',
        'scalePolicy': 'cru',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
        'lbConfig': 'cru',
    })


def test_svc_discovery_consume_map(admin_user_client, user_client,
                                   project_client):
    auth_check(admin_user_client.schema, 'serviceConsumeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'consumedServiceId': 'r',
        'ports': 'r',
        'data': 'r',
        'accountId': 'r'
    })

    auth_check(user_client.schema, 'serviceConsumeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'consumedServiceId': 'r',
        'ports': 'r',
        'accountId': 'r'
    })

    auth_check(project_client.schema, 'serviceConsumeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'consumedServiceId': 'r',
        'ports': 'r',
        'accountId': 'r',
    })


def test_auth_k8s_stack_upgrade(admin_user_client, user_client,
                                project_client):
    auth_check(admin_user_client.schema, 'kubernetesStackUpgrade', 'r', {
        'templates': 'r',
        'environment': 'r',
        'answers': 'r',
        'externalId': 'r',
    })

    auth_check(user_client.schema, 'kubernetesStackUpgrade', 'r', {
        'templates': 'r',
        'environment': 'r',
        'answers': 'r',
        'externalId': 'r',
    })

    auth_check(project_client.schema, 'kubernetesStackUpgrade', 'cr', {
        'templates': 'cr',
        'environment': 'cr',
        'externalId': 'cr',
        'answers': 'cr',
    })


def test_auth_env_upgrade(admin_user_client, user_client,
                          project_client):
    auth_check(admin_user_client.schema, 'stackUpgrade', 'r', {
        'dockerCompose': 'r',
        'rancherCompose': 'r',
        'environment': 'r',
        'externalId': 'r',
        'answers': 'r',
        'templates': 'r',
    })

    auth_check(user_client.schema, 'stackUpgrade', 'r', {
        'dockerCompose': 'r',
        'rancherCompose': 'r',
        'environment': 'r',
        'externalId': 'r',
        'answers': 'r',
        'templates': 'r',
    })

    auth_check(project_client.schema, 'stackUpgrade', 'cr', {
        'dockerCompose': 'cr',
        'rancherCompose': 'cr',
        'environment': 'cr',
        'externalId': 'cr',
        'answers': 'cr',
        'templates': 'cr',
    })


def test_auth_service_upgrade(admin_user_client, user_client,
                              project_client):
    auth_check(admin_user_client.schema, 'serviceUpgrade', 'r', {
        'inServiceStrategy': 'r',
        'toServiceStrategy': 'r'
    })

    auth_check(user_client.schema, 'serviceUpgrade', 'r', {
        'inServiceStrategy': 'r',
        'toServiceStrategy': 'r'
    })

    auth_check(project_client.schema, 'serviceUpgrade', 'cr', {
        'inServiceStrategy': 'cr',
        'toServiceStrategy': 'cr'
    })


def test_auth_in_service_upgrade_strategy(admin_user_client, user_client,
                                          project_client):
    auth_check(admin_user_client.schema, 'inServiceUpgradeStrategy', 'r', {
        'batchSize': 'r',
        'intervalMillis': 'r',
        'launchConfig': 'r',
        'secondaryLaunchConfigs': 'r',
        'previousLaunchConfig': 'r',
        'previousSecondaryLaunchConfigs': 'r',
        'startFirst': 'r',
    })

    auth_check(user_client.schema, 'inServiceUpgradeStrategy', 'r', {
        'batchSize': 'r',
        'intervalMillis': 'r',
        'launchConfig': 'r',
        'secondaryLaunchConfigs': 'r',
        'previousLaunchConfig': 'r',
        'previousSecondaryLaunchConfigs': 'r',
        'startFirst': 'r',
    })

    auth_check(project_client.schema, 'inServiceUpgradeStrategy', 'cr', {
        'batchSize': 'cr',
        'intervalMillis': 'cr',
        'launchConfig': 'cr',
        'secondaryLaunchConfigs': 'cr',
        'previousLaunchConfig': 'r',
        'previousSecondaryLaunchConfigs': 'r',
        'startFirst': 'cr',
    })


def test_auth_to_service_upgrade_strategy(admin_user_client, user_client,
                                          project_client):
    auth_check(admin_user_client.schema, 'toServiceUpgradeStrategy', 'r', {
        'updateLinks': 'r',
        'toServiceId': 'r',
        'batchSize': 'r',
        'intervalMillis': 'r',
        'finalScale': 'r'
    })

    auth_check(user_client.schema, 'toServiceUpgradeStrategy', 'r', {
        'updateLinks': 'r',
        'toServiceId': 'r',
        'batchSize': 'r',
        'intervalMillis': 'r',
        'finalScale': 'r'
    })

    auth_check(project_client.schema, 'toServiceUpgradeStrategy', 'cr', {
        'updateLinks': 'cr',
        'toServiceId': 'cr',
        'batchSize': 'cr',
        'intervalMillis': 'cr',
        'finalScale': 'cr'
    })


def test_svc_discovery_expose_map(admin_user_client, user_client,
                                  project_client):
    auth_check(admin_user_client.schema, 'serviceExposeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'ipAddress': 'r',
        'data': 'r',
        'accountId': 'r',
        'managed': 'r'
    })

    auth_check(user_client.schema, 'serviceExposeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'ipAddress': 'r',
        'accountId': 'r',
        'managed': 'r'
    })

    auth_check(project_client.schema, 'serviceExposeMap', 'r', {
        'name': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'ipAddress': 'r',
        'accountId': 'r',
        'managed': 'r'
    })


def test_svc_discovery_external_service(admin_user_client, user_client,
                                        project_client):
    auth_check(admin_user_client.schema, 'externalService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'hostname': 'r',
        'externalIpAddresses': 'r',
        'accountId': 'r',
        'data': 'r',
        'upgrade': 'r',
        'healthCheck': 'r',
        'metadata': 'r',
        'launchConfig': 'r',
        'fqdn': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(user_client.schema, 'externalService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'hostname': 'r',
        'externalIpAddresses': 'r',
        'accountId': 'r',
        'upgrade': 'r',
        'healthCheck': 'r',
        'metadata': 'r',
        'launchConfig': 'r',
        'fqdn': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(project_client.schema, 'externalService', 'crud', {
        'name': 'cru',
        'externalId': 'cr',
        'stackId': 'cr',
        'hostname': 'cru',
        'externalIpAddresses': 'cru',
        'accountId': 'r',
        'upgrade': 'r',
        'healthCheck': 'cr',
        'metadata': 'cru',
        'launchConfig': 'cr',
        'fqdn': 'r',
        'healthState': 'r',
        'startOnCreate': 'cr',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })


def test_pull_task(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'pullTask', 'r', {
        'accountId': 'r',
        'data': 'r',
        'labels': 'r',
        'mode': 'r',
        'image': 'r',
        'status': 'r',
    })

    auth_check(user_client.schema, 'pullTask', 'r', {
        'accountId': 'r',
        'labels': 'r',
        'mode': 'r',
        'image': 'r',
        'status': 'r',
    })

    auth_check(project_client.schema, 'pullTask', 'cr', {
        'accountId': 'r',
        'labels': 'cr',
        'mode': 'cr',
        'image': 'cr',
        'status': 'r',
        'kind': 'r',
    })


def test_generic_object(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'genericObject', 'crd', {
        'name': 'cr',
        'kind': 'cr',
        'key': 'cr',
        'accountId': 'r',
        'data': 'r',
        'resourceData': 'cr'
    })

    auth_check(user_client.schema, 'genericObject', 'r', {
        'name': 'r',
        'kind': 'r',
        'key': 'r',
        'accountId': 'r',
        'resourceData': 'r'
    })

    auth_check(project_client.schema, 'genericObject', 'crd', {
        'name': 'cr',
        'kind': 'cr',
        'key': 'cr',
        'accountId': 'r',
        'resourceData': 'cr'
    })


def test_external_event(agent_client, admin_user_client, user_client,
                        project_client):
    auth_check(admin_user_client.schema, 'externalEvent', 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
    })

    auth_check(user_client.schema, 'externalEvent', 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
    })

    auth_check(project_client.schema, 'externalEvent', 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
    })

    auth_check(agent_client.schema, 'externalEvent', 'r', {
        'externalId': 'r',
        'eventType': 'r',
    })


def test_external_sp_event(agent_client, admin_user_client, user_client,
                           project_client):
    type = 'externalStoragePoolEvent'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'hostUuids': 'r',
        'reportedAccountId': 'r',
        'storagePool': 'r',
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'hostUuids': 'r',
        'reportedAccountId': 'r',
        'storagePool': 'r',
    })

    auth_check(project_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'hostUuids': 'r',
        'reportedAccountId': 'r',
        'storagePool': 'r',
    })

    auth_check(agent_client.schema, type, 'cr', {
        'externalId': 'cr',
        'eventType': 'cr',
        'hostUuids': 'cr',
        'storagePool': 'cr',
    })


def test_external_volume_event(agent_client, admin_user_client, user_client,
                               project_client):
    type = 'externalVolumeEvent'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'volume': 'r',
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'volume': 'r',
    })

    auth_check(project_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'volume': 'r',
    })

    auth_check(agent_client.schema, type, 'cr', {
        'externalId': 'cr',
        'eventType': 'cr',
        'volume': 'cr',
    })


def test_external_dns_event(agent_client, admin_user_client, user_client,
                            project_client):
    type = 'externalDnsEvent'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'stackName': 'r',
        'serviceName': 'r',
        'fqdn': 'r'
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'stackName': 'r',
        'serviceName': 'r',
        'fqdn': 'r'
    })

    auth_check(project_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'stackName': 'r',
        'serviceName': 'r',
        'fqdn': 'r'
    })

    auth_check(agent_client.schema, type, 'cr', {
        'externalId': 'cr',
        'eventType': 'cr',
        'stackName': 'cr',
        'serviceName': 'cr',
        'fqdn': 'cr'
    })


def test_external_service_event(agent_client, admin_user_client, user_client,
                                project_client):
    type = 'externalServiceEvent'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'environment': 'r',
        'service': 'r',
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'environment': 'r',
        'service': 'r',
    })

    auth_check(project_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'environment': 'r',
        'service': 'r',
    })

    auth_check(agent_client.schema, type, 'cr', {
        'externalId': 'cr',
        'eventType': 'cr',
        'environment': 'cr',
        'service': 'cr',
    })


def test_network(agent_client, admin_user_client, user_client,
                 project_client):
    type = 'network'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'metadata': 'r',
        'subnets': 'r',
        'networkDriverId': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'hostPorts': 'r',
        'defaultPolicyAction': 'r',
        'policy': 'r',
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'metadata': 'r',
        'subnets': 'r',
        'networkDriverId': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'hostPorts': 'r',
        'defaultPolicyAction': 'r',
        'policy': 'r',
    })

    auth_check(project_client.schema, type, 'crud', {
        'accountId': 'r',
        'metadata': 'cru',
        'subnets': 'cr',
        'networkDriverId': 'cr',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'hostPorts': 'cr',
        'defaultPolicyAction': 'cru',
        'policy': 'cru',
    })


def test_external_host_event(agent_client, admin_user_client, user_client,
                             project_client):
    type = 'externalHostEvent'

    auth_check(admin_user_client.schema, type, 'r', {
        'accountId': 'r',
        'data': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'hostLabel': 'r',
        'deleteHost': 'r',
        'hostId': 'r',
    })

    auth_check(user_client.schema, type, 'r', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'r',
        'reportedAccountId': 'r',
        'hostLabel': 'r',
        'deleteHost': 'r',
        'hostId': 'r',
    })

    auth_check(project_client.schema, type, 'cr', {
        'accountId': 'r',
        'externalId': 'r',
        'eventType': 'cr',
        'reportedAccountId': 'r',
        'hostLabel': 'cr',
        'deleteHost': 'cr',
        'hostId': 'cr',
    })


def test_virtual_machine(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'virtualMachine', 'r', {
        'accountId': 'r',
        'agentId': 'r',
        'allocationState': 'r',
        'blkioDeviceOptions': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'data': 'r',
        'description': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'memory': 'r',
        'memoryReservation': 'r',
        'memorySwap': 'r',
        'mounts': 'r',
        'milliCpuReservation': 'r',
        'networkIds': 'r',
        'networkMode': 'r',
        'ports': 'r',
        'userPorts': 'r',
        'primaryIpAddress': 'r',
        'removeTime': 'r',
        'registryCredentialId': 'r',
        'requestedHostId': 'r',
        'restartPolicy': 'r',
        'startOnCreate': 'r',
        'token': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r',
        'healthCheck': 'r',
        'healthState': 'r',
        'securityOpt': 'r',
        'logConfig': 'r',
        'volumeDriver': 'r',
        'extraHosts': 'r',
        'expose': 'r',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'startCount': 'r',
        'vcpu': 'r',
        'userdata': 'r',
        'memoryMb': 'r',
        'disks': 'r',
        'serviceIds': 'r',
        'system': 'r',
        'blkioWeight': 'r',
        'cgroupParent': 'r',
        'usernsMode': 'r',
        'pidsLimit': 'r',
        'diskQuota': 'r',
        'cpuCount': 'r',
        'cpuPercent': 'r',
        'ioMaximumIOps': 'r',
        'ioMaximumBandwidth': 'r',
        'cpuPeriod': 'r',
        'cpuQuota': 'r',
        'cpuSetMems': 'r',
        'dnsOpt': 'r',
        'groupAdd': 'r',
        'isolation': 'r',
        'kernelMemory': 'r',
        'memorySwappiness': 'r',
        'oomKillDisable': 'r',
        'shmSize': 'r',
        'uts': 'r',
        'ipcMode': 'r',
        'stopSignal': 'r',
        'sysctls': 'r',
        'storageOpt': 'r',
        'oomScoreAdj': 'r',
        'ulimits': 'r',
        'tmpfs': 'r',
        'ip': 'r',
        'ip6': 'r',
        'netAlias': 'r',
        'healthCmd': 'r',
        'healthInterval': 'r',
        'healthTimeout': 'r',
        'healthRetries': 'r',
        'instanceTriggeredStop': 'r',
        'primaryNetworkId': 'r',
        'serviceId': 'r',
        'stackId': 'r',
    })

    auth_check(user_client.schema, 'virtualMachine', 'r', {
        'accountId': 'r',
        'blkioDeviceOptions': 'r',
        'command': 'r',
        'count': 'r',
        'cpuSet': 'r',
        'cpuShares': 'r',
        'created': 'r',
        'description': 'r',
        'dns': 'r',
        'dnsSearch': 'r',
        'domainName': 'r',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'r',
        'id': 'r',
        'imageUuid': 'r',
        'instanceLinks': 'r',
        'memory': 'r',
        'memoryReservation': 'r',
        'memorySwap': 'r',
        'milliCpuReservation': 'r',
        'mounts': 'r',
        'networkIds': 'r',
        'networkMode': 'r',
        'ports': 'r',
        'userPorts': 'r',
        'primaryIpAddress': 'r',
        'registryCredentialId': 'r',
        'requestedHostId': 'r',
        'restartPolicy': 'r',
        'startOnCreate': 'r',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'r',
        'healthCheck': 'r',
        'healthState': 'r',
        'securityOpt': 'r',
        'logConfig': 'r',
        'extraHosts': 'r',
        'volumeDriver': 'r',
        'expose': 'r',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'startCount': 'r',
        'vcpu': 'r',
        'userdata': 'r',
        'memoryMb': 'r',
        'disks': 'r',
        'serviceIds': 'r',
        'system': 'r',
        'blkioWeight': 'r',
        'cgroupParent': 'r',
        'usernsMode': 'r',
        'pidsLimit': 'r',
        'diskQuota': 'r',
        'cpuCount': 'r',
        'cpuPercent': 'r',
        'ioMaximumIOps': 'r',
        'ioMaximumBandwidth': 'r',
        'cpuPeriod': 'r',
        'cpuQuota': 'r',
        'cpuSetMems': 'r',
        'dnsOpt': 'r',
        'groupAdd': 'r',
        'isolation': 'r',
        'kernelMemory': 'r',
        'memorySwappiness': 'r',
        'oomKillDisable': 'r',
        'shmSize': 'r',
        'uts': 'r',
        'ipcMode': 'r',
        'stopSignal': 'r',
        'sysctls': 'r',
        'storageOpt': 'r',
        'oomScoreAdj': 'r',
        'ulimits': 'r',
        'tmpfs': 'r',
        'ip': 'r',
        'ip6': 'r',
        'netAlias': 'r',
        'healthCmd': 'r',
        'healthInterval': 'r',
        'healthTimeout': 'r',
        'healthRetries': 'r',
        'instanceTriggeredStop': 'r',
        'primaryNetworkId': 'r',
        'serviceId': 'r',
        'stackId': 'r',
    })

    auth_check(project_client.schema, 'virtualMachine', 'crud', {
        'accountId': 'r',
        'blkioDeviceOptions': 'cr',
        'command': 'cr',
        'count': 'cr',
        'cpuSet': 'cr',
        'cpuShares': 'cr',
        'created': 'r',
        'description': 'cru',
        'dns': 'cr',
        'dnsSearch': 'cr',
        'domainName': 'cr',
        'firstRunning': 'r',
        'hostId': 'r',
        'hostname': 'cr',
        'id': 'r',
        'imageUuid': 'cr',
        'instanceLinks': 'cr',
        'memory': 'cr',
        'memoryReservation': 'cr',
        'memorySwap': 'cr',
        'milliCpuReservation': 'cr',
        'mounts': 'r',
        'networkIds': 'cr',
        'networkMode': 'cr',
        'ports': 'cr',
        'userPorts': 'r',
        'primaryIpAddress': 'r',
        'registryCredentialId': 'cr',
        'requestedHostId': 'cr',
        'restartPolicy': 'cr',
        'startOnCreate': 'cr',
        'nativeContainer': 'r',
        'externalId': 'r',
        'labels': 'cr',
        'healthCheck': 'cr',
        'healthState': 'r',
        'securityOpt': 'cr',
        'logConfig': 'cr',
        'extraHosts': 'cr',
        'volumeDriver': 'cr',
        'expose': 'cr',
        'createIndex': 'r',
        'deploymentUnitUuid': 'r',
        'version': 'r',
        'startCount': 'r',
        'vcpu': 'cr',
        'userdata': 'cr',
        'memoryMb': 'cr',
        'disks': 'cr',
        'serviceIds': 'r',
        'system': 'r',
        'blkioWeight': 'cr',
        'cgroupParent': 'cr',
        'usernsMode': 'cr',
        'pidsLimit': 'cr',
        'diskQuota': 'cr',
        'cpuCount': 'cr',
        'cpuPercent': 'cr',
        'ioMaximumIOps': 'cr',
        'ioMaximumBandwidth': 'cr',
        'cpuPeriod': 'cr',
        'cpuQuota': 'cr',
        'cpuSetMems': 'cr',
        'dnsOpt': 'cr',
        'groupAdd': 'cr',
        'isolation': 'cr',
        'kernelMemory': 'cr',
        'memorySwappiness': 'cr',
        'oomKillDisable': 'cr',
        'shmSize': 'cr',
        'uts': 'cr',
        'ipcMode': 'cr',
        'stopSignal': 'cr',
        'sysctls': 'cr',
        'storageOpt': 'cr',
        'oomScoreAdj': 'cr',
        'ulimits': 'cr',
        'tmpfs': 'cr',
        'ip': 'cr',
        'ip6': 'cr',
        'netAlias': 'cr',
        'healthCmd': 'cr',
        'healthInterval': 'cr',
        'healthTimeout': 'cr',
        'healthRetries': 'cr',
        'instanceTriggeredStop': 'cr',
        'primaryNetworkId': 'r',
        'serviceId': 'r',
        'stackId': 'cr',
    })


def test_virtual_machine_disk(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'virtualMachineDisk', 'r', {
        'name': 'r',
        'size': 'r',
        'readIops': 'r',
        'writeIops': 'r',
        'opts': 'r',
        'driver': 'r',
        'root': 'r',
    })

    auth_check(user_client.schema, 'virtualMachineDisk', 'r', {
        'name': 'r',
        'size': 'r',
        'readIops': 'r',
        'writeIops': 'r',
        'opts': 'r',
        'driver': 'r',
        'root': 'r',
    })

    auth_check(project_client.schema, 'virtualMachineDisk', 'cr', {
        'name': 'cr',
        'size': 'cr',
        'readIops': 'cr',
        'writeIops': 'cr',
        'opts': 'cr',
        'driver': 'cr',
        'root': 'cr',
    })


def test_compose_service(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'composeService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'data': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'launchConfig': 'r',
        'fqdn': 'r',
        'selectorLink': 'r',
        'scale': 'r',
        'publicEndpoints': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(user_client.schema, 'composeService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'launchConfig': 'r',
        'fqdn': 'r',
        'selectorLink': 'r',
        'scale': 'r',
        'publicEndpoints': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(project_client.schema, 'composeService', 'rd', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'healthState': 'r',
        'startOnCreate': 'r',
        'launchConfig': 'r',
        'fqdn': 'r',
        'selectorLink': 'r',
        'scale': 'r',
        'publicEndpoints': 'r',
        'scalePolicy': 'r',
        'currentScale': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })


def test_machine_driver(admin_user_client, user_client, project_client,
                        service_client):
    auth_check(admin_user_client.schema, 'machineDriver', 'crud', {
        'name': 'r',
        'checksum': 'cru',
        'externalId': 'cru',
        'uiUrl': 'cru',
        'data': 'r',
        'url': 'cru',
        'defaultActive': 'r',
        'builtin': 'cr',
        'activateOnCreate': 'cr',
    })

    auth_check(service_client.schema, 'machineDriver', 'crud', {
        'name': 'cru',
        'checksum': 'cru',
        'externalId': 'cru',
        'uiUrl': 'cru',
        'data': 'r',
        'url': 'cru',
        'defaultActive': 'r',
        'builtin': 'cr',
        'activateOnCreate': 'cr',
        'schemaVersion': 'cru',
    })

    auth_check(user_client.schema, 'machineDriver', 'r', {
        'name': 'r',
        'checksum': 'r',
        'externalId': 'r',
        'uiUrl': 'r',
        'url': 'r',
        'defaultActive': 'r',
        'builtin': 'r',
        'activateOnCreate': 'r',
    })

    auth_check(project_client.schema, 'machineDriver', 'r', {
        'name': 'r',
        'checksum': 'r',
        'externalId': 'r',
        'uiUrl': 'r',
        'url': 'r',
        'defaultActive': 'r',
        'builtin': 'r',
        'activateOnCreate': 'r',
    })


def test_kubernetes_service(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'kubernetesService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'data': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'template': 'r',
        'healthState': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(user_client.schema, 'kubernetesService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'template': 'r',
        'healthState': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })

    auth_check(project_client.schema, 'kubernetesService', 'r', {
        'name': 'r',
        'externalId': 'r',
        'stackId': 'r',
        'accountId': 'r',
        'vip': 'r',
        'selectorContainer': 'r',
        'template': 'r',
        'healthState': 'r',
        'instanceIds': 'r',
        'linkedServices': 'r',
        'system': 'r',
    })


def test_machine(admin_user_client, user_client, project_client,
                 service_client):
    auth_check(project_client.schema, 'machine', 'crd', {
        'accountId': 'r',
        'name': 'cr',
        'driver': 'r',
        'externalId': 'r',
        'engineRegistryMirror': 'cr',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'labels': 'cr',
        'engineInstallUrl': 'cr',
        'dockerVersion': 'cr',
        'engineOpt': 'cr',
        'engineInsecureRegistry': 'cr',
        'engineLabel': 'cr',
        'engineStorageDriver': 'cr',
        'engineEnv': 'cr',
    })

    auth_check(service_client.schema, 'machine', 'crud', {
        'accountId': 'r',
        'name': 'cr',
        'driver': 'r',
        'data': 'r',
        'extractedConfig': 'ru',
        'externalId': 'r',
        'engineRegistryMirror': 'cr',
        'authCertificateAuthority': 'cr',
        'authKey': 'cr',
        'labels': 'cru',
        'engineInstallUrl': 'cr',
        'dockerVersion': 'cr',
        'engineOpt': 'cr',
        'engineInsecureRegistry': 'cr',
        'engineLabel': 'cr',
        'engineStorageDriver': 'cr',
        'engineEnv': 'cr',
    })

    auth_check(admin_user_client.schema, 'machine', 'r', {
        'accountId': 'r',
        'name': 'r',
        'data': 'r',
        'driver': 'r',
        'externalId': 'r',
        'engineRegistryMirror': 'r',
        'authCertificateAuthority': 'r',
        'authKey': 'r',
        'labels': 'r',
        'engineInstallUrl': 'r',
        'dockerVersion': 'r',
        'engineOpt': 'r',
        'engineInsecureRegistry': 'r',
        'engineLabel': 'r',
        'engineStorageDriver': 'r',
        'engineEnv': 'r',
    })

    auth_check(user_client.schema, 'machine', 'r', {
        'accountId': 'r',
        'name': 'r',
        'driver': 'r',
        'externalId': 'r',
        'engineRegistryMirror': 'r',
        'authCertificateAuthority': 'r',
        'authKey': 'r',
        'labels': 'r',
        'engineInstallUrl': 'r',
        'dockerVersion': 'r',
        'engineOpt': 'r',
        'engineInsecureRegistry': 'r',
        'engineLabel': 'r',
        'engineStorageDriver': 'r',
        'engineEnv': 'r',
    })


def test_snapshot_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'snapshot', 'r', {
        'accountId': 'r',
        'data': 'r',
        'volumeId': 'r',
        })

    auth_check(user_client.schema, 'snapshot', 'r', {
        'accountId': 'r',
        'volumeId': 'r',
        })

    auth_check(project_client.schema, 'snapshot', 'rd', {
        'accountId': 'r',
        'volumeId': 'r',
        })


def test_backup_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'backup', 'r', {
        'accountId': 'r',
        'data': 'r',
        'backupTargetId': 'r',
        'snapshotId': 'r',
        'uri': 'r',
        'volumeId': 'r',
        })

    auth_check(user_client.schema, 'backup', 'r', {
        'accountId': 'r',
        'backupTargetId': 'r',
        'snapshotId': 'r',
        'uri': 'r',
        'volumeId': 'r',
        })

    auth_check(project_client.schema, 'backup', 'rd', {
        'accountId': 'r',
        'backupTargetId': 'r',
        'snapshotId': 'r',
        'uri': 'r',
        'volumeId': 'r',
        })


def test_backup_target_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'backupTarget', 'r', {
        'accountId': 'r',
        'data': 'r',
        'nfsConfig': 'r',
    })

    auth_check(user_client.schema, 'backupTarget', 'r', {
        'accountId': 'r',
        'nfsConfig': 'r',
    })

    auth_check(project_client.schema, 'backupTarget', 'crd', {
        'accountId': 'r',
        'nfsConfig': 'cr',
    })


def test_service_log(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'serviceLog', 'r', {
        'accountId': 'r',
        'data': 'r',
        'endTime': 'r',
        'eventType': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'transactionId': 'r',
        'subLog': 'r',
        'level': 'r',
    })

    auth_check(user_client.schema, 'serviceLog', 'r', {
        'accountId': 'r',
        'endTime': 'r',
        'eventType': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'transactionId': 'r',
        'subLog': 'r',
        'level': 'r',
    })

    auth_check(project_client.schema, 'serviceLog', 'r', {
        'accountId': 'r',
        'endTime': 'r',
        'eventType': 'r',
        'serviceId': 'r',
        'instanceId': 'r',
        'transactionId': 'r',
        'subLog': 'r',
        'level': 'r',
    })


def test_storage_driver_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'storageDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'scope': 'r',
        'data': 'r',
    })

    auth_check(user_client.schema, 'storageDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'scope': 'r',
    })

    auth_check(project_client.schema, 'storageDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'volumeAccessMode': 'r',
        'blockDevicePath': 'r',
        'volumeCapabilities': 'r',
        'scope': 'r',
    })


def test_network_driver_auth(admin_user_client, user_client, project_client):
    auth_check(admin_user_client.schema, 'networkDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'data': 'r',
        'cniConfig': 'r',
        'networkMetadata': 'r',
        'defaultNetwork': 'r',
    })

    auth_check(user_client.schema, 'networkDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'cniConfig': 'r',
        'networkMetadata': 'r',
        'defaultNetwork': 'r',
    })

    auth_check(project_client.schema, 'networkDriver', 'r', {
        'accountId': 'r',
        'serviceId': 'r',
        'cniConfig': 'r',
        'networkMetadata': 'r',
        'defaultNetwork': 'r',
    })
