from common_fixtures import *  # NOQA
from cattle import ApiError
from test_physical_host import disable_go_machine_service  # NOQA
from copy import deepcopy


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


@pytest.fixture(scope='module')
def machine_context(admin_user_client):
    return create_context(admin_user_client, create_project=True,
                          add_host=True)


@pytest.fixture(scope='module')
def admin_client(machine_context):
    return machine_context.client


@pytest.fixture(scope='module')
def admin_account(machine_context):
    return machine_context.project


def test_machine_lifecycle(super_client, admin_client, admin_account,
                           update_ping_settings):
    name = random_str()
    machine = admin_client.create_machine(name=name,
                                          virtualboxConfig={})

    machine = admin_client.wait_success(machine)
    assert machine.state == 'active'
    assert machine.virtualboxConfig is not None

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
    account_id = get_plain_id(super_client, admin_account)
    data[scope]['agentResourcesAccountId'] = account_id
    data['agentResourcesAccountId'] = account_id

    agent = super_client.create_agent(uri=uri, data=data)
    agent = super_client.wait_success(agent)

    wait_for(lambda: len(agent.hosts()) == 1)
    hosts = agent.hosts()

    assert len(hosts) == 1
    host = hosts[0]
    assert host.physicalHostId == machine.id
    assert machine.accountId == host.accountId

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

    machine = admin_client.wait_success(machine.remove())
    assert machine.state == 'removed'

    agent = super_client.wait_success(super_client.reload(machine).agent())
    assert agent.state == 'removed'

    host = admin_client.wait_success(admin_client.reload(host))
    assert host.state == 'removed'


def test_machine_driver_config(admin_client):
    name = "test-%s" % random_str()
    vbox_config = {
        "memory": "2048",
        "diskSize": "40000",
        "boot2dockerUrl": "http://localhost/random",
    }
    ca = "ca-1"
    key = "key-1"
    host = admin_client.create_machine(name=name,
                                       virtualboxConfig=vbox_config,
                                       authCertificateAuthority=ca,
                                       authKey=key)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert vbox_config == host.virtualboxConfig
    assert ca == host.authCertificateAuthority
    assert key == host.authKey
    assert host.driver == 'virtualbox'

    name = "test-%s" % random_str()
    digoc_config = {
        "image": "img1",
        "region": "reg1",
        "size": "40000",
        "accessToken": "ac-1",
        "ipv6": True,
        "privateNetworking": True,
        "backups": True
    }
    host = admin_client.create_machine(name=name,
                                       digitaloceanConfig=digoc_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert digoc_config == host.digitaloceanConfig
    assert host.driver == 'digitalocean'

    name = "test-%s" % random_str()
    ec2_config = {
        "accessKey": "accesskey1",
        "ami": "ami1",
        "iamInstanceProfile": "profile1",
        "instanceType": "type1",
        "monitoring": False,
        "privateAddressOnly": False,
        "region": "us-east-1",
        "requestSpotInstance": False,
        "rootSize": "60GB",
        "secretKey": "secretkey1",
        "securityGroup": "docker-machine",
        "sessionToken": "sessiontoken1",
        "spotPrice": "spotPrice1",
        "sshUser": "sshUser1",
        "subnetId": "5678",
        "usePrivateAddress": False,
        "vpcId": "1234",
        "zone": "us-east-1a",
    }
    host = admin_client.create_machine(name=name,
                                       amazonec2Config=ec2_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert ec2_config == host.amazonec2Config
    assert host.driver == 'amazonec2'

    name = "test-%s" % random_str()
    packet_config = {
        "apiKey": "apikey1",
        "billingCycle": "hourly",
        "facilityCode": "ewr1",
        "os": "centos_7",
        "plan": "baremetal_1",
        "projectId": "projectId",
    }
    host = admin_client.create_machine(name=name,
                                       packetConfig=packet_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert packet_config == host.packetConfig
    assert host.driver == 'packet'

    name = "test-%s" % random_str()
    azure_config = {
        "dockerPort": "dockerPort",
        "dockerSwarmMasterPort": "dockerSwarmMasterPort1",
        "image": "image",
        "location": "location",
        "password": "password",
        "publishSettingsFile": "publishSettingsFile",
        "size": "size",
        "sshPort": "sshPort1",
        "subscriptionCert": "subscriptionCert",
        "subscriptionId": "subscriptionId",
        "username": "username",
    }
    host = admin_client.create_machine(name=name,
                                       azureConfig=azure_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert azure_config == host.azureConfig
    assert host.driver == 'azure'

    name = "test-%s" % random_str()
    rackspace_config = {
        "apiKey": "apiKey",
        "dockerInstall": "dockerInstall",
        "endpointType": "endpointType",
        "flavorId": "flavorId",
        "imageId": "imageId",
        "region": "region",
        "sshPort": "sshPort",
        "sshUser": "sshUser",
        "username": "username",
    }
    host = admin_client.create_machine(name=name,
                                       rackspaceConfig=rackspace_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert rackspace_config == host.rackspaceConfig
    assert host.driver == 'rackspace'


def test_machine_validation(admin_client):
    name = "test-%s" % random_str()

    # Can't set two drivers
    try:
        admin_client.create_machine(name=name,
                                    virtualboxConfig={},
                                    digitaloceanConfig={"accessToken": "a"})
    except ApiError as e:
        assert e.error.status == 422
        assert e.error.code == 'DriverConfigExactlyOneRequired'
    else:
        assert False, "Should not have been able to set two drivers."

    # Must set at least one driver
    try:
        admin_client.create_machine(name=name)
    except ApiError as e:
        assert e.error.status == 422
        assert e.error.code == 'DriverConfigExactlyOneRequired'
    else:
        assert False, "Should have been required to set a driver."

    # Property present, but None/nil/null is acceptable
    host = admin_client.create_machine(name=name,
                                       virtualboxConfig={},
                                       digitaloceanConfig=None)
    assert host is not None


def test_digitalocean_config_validation(admin_client):
    name = "test-%s" % random_str()

    # accessToken is required
    try:
        admin_client.create_machine(name=name,
                                    digitaloceanConfig={})
    except ApiError as e:
        assert e.error.status == 422
        assert e.error.code == 'MissingRequired'
    else:
        assert False, 'Should have got MissingRequired for accessToken'


def test_config_link_readonly(admin_user_client, super_client, request):
    user1_context = create_context(admin_user_client, create_project=True)
    user2_context = new_context(admin_user_client, request)

    project = user1_context.user_client.reload(user1_context.project)

    members = get_plain_members(project.projectMembers())
    members.append({
        'role': 'readonly',
        'externalId': user2_context.account.id,
        'externalIdType': 'rancher_id'
    })
    project.setmembers(members=members)

    user1_client = user1_context.user_client
    user2_client = user2_context.user_client

    new_headers = deepcopy(user1_client._headers)
    new_headers['X-API-Project-Id'] = project.id

    user1_client._headers = new_headers
    user2_client._headers = new_headers

    user1_client.reload_schema()
    user2_client.reload_schema()

    name = "test-%s" % random_str()
    digoc_config = {
        "image": "img1",
        "region": "reg1",
        "size": "40000",
        "accessToken": "ac-1",
        "ipv6": True,
        "privateNetworking": True,
        "backups": True
    }

    host = user1_client.create_machine(name=name,
                                       digitaloceanConfig=digoc_config)
    host = user1_client.wait_success(host)
    host = super_client.by_id('physicalHost', host.id)

    super_client.update(host, extractedConfig='hello')

    host = super_client.reload(host)
    assert 'config' in host.links
    host = user1_client.reload(host)
    assert 'config' in host.links
    host = user2_client.reload(host)
    assert 'config' not in host.links
