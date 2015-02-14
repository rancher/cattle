from common_fixtures import *  # NOQA
from cattle import ApiError
from test_physical_host import disable_go_machine_service  # NOQA


def test_machine_lifecycle(super_client, admin_client):
    name = "test-%s" % random_str()
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
    agent = super_client.create_agent(uri=uri, data=data)
    agent = super_client.wait_success(agent)
    hosts = agent.hosts()

    assert len(hosts) == 1
    host = hosts[0]
    assert host.physicalHostId == machine.id

    # TODO When https://github.com/rancherio/cattle/pull/191 is merged,
    # make use of the refactored user contexts that create account
    # specific hosts. These asserts should then pass:
    # assert machine.accountId == host.accountId
    # assert machine.accountId == agent.accountId

    machine = admin_client.wait_success(machine.remove())
    assert machine.state == 'removed'

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
    }
    host = admin_client.create_machine(name=name,
                                       digitaloceanConfig=digoc_config)
    host = admin_client.wait_success(host)
    assert host.state == 'active'
    assert digoc_config == host.digitaloceanConfig
    assert host.driver == 'digitalocean'


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
