from cattle import ApiError

from common_fixtures import *  # NOQA


def test_virtual_machine_create_cpu_memory(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     vcpu=2,
                                                     memoryMb=42)

    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu == 2
    assert vm.memoryMb == 42


def test_virtual_machine_create(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid)

    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu is None
    assert vm.memoryMb == 256


def test_virtual_machine_create_null_network_id(internal_test_client,
                                                sim_context):
    image_uuid = sim_context['imageUuid']
    try:
        internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                    networkIds=[None])
        assert False
    except ApiError as e:
        assert e.error.code == 'NotNullable'


def test_virtual_machine_n_ids_s_ids(internal_test_client, sim_context,
                                     network, subnet):
    image_uuid = sim_context['imageUuid']
    try:
        internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                    networkIds=[network.id],
                                                    subnetIds=[subnet.id])
    except ApiError as e:
        assert e.error.code == 'NetworkIdsSubnetIdsMutuallyExclusive'


def test_virtual_machine_network(internal_test_client, sim_context, network,
                                 subnet):
    subnet_plain_id = get_plain_id(internal_test_client, subnet)
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     networkIds=[network.id])

    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'
    assert 'networkIds' not in vm

    nics = vm.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.network().id == network.id
    assert nic.state == 'active'
    assert nic.macAddress is not None
    assert nic.macAddress.startswith(network.macPrefix)

    nic_admin = internal_test_client.reload(nic)
    vm_admin = internal_test_client.reload(vm)

    assert nic_admin.account().id == vm_admin.accountId

    ips = nic.ipAddresses()

    assert len(ips) == 1
    assert internal_test_client.reload(nic).ipAddressNicMaps()[0].state == \
        'active'

    ip = ips[0]
    ip_admin = internal_test_client.reload(ip)

    assert ip_admin.account().id == vm_admin.accountId
    assert ip_admin.subnet().id == nic_admin.subnet().id
    assert ip_admin.role == 'primary'

    assert ip.address is not None
    assert ip.address.startswith('192.168.0')

    assert vm.primaryIpAddress is not None
    assert vm.primaryIpAddress == ip.address

    addresses = internal_test_client.list_resource_pool(poolType='subnet',
                                                        poolId=subnet_plain_id)
    assert ip.address in [x.item for x in addresses]


def test_virtual_machine_subnet(internal_test_client, sim_context, subnet,
                                vnet):
    network = subnet.network()
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     subnetIds=[subnet.id])

    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'
    assert 'subnetIds' not in vm

    nics = vm.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.subnetId == subnet.id
    assert nic.network().id == network.id
    assert nic.state == 'active'

    ips = nic.ipAddresses()

    assert len(ips) == 1

    ip = ips[0]

    assert ip.address is not None
    assert ip.address.startswith('192.168.0')

    assert vm.primaryIpAddress is not None
    assert vm.primaryIpAddress == ip.address


def test_virtual_machine_no_ip(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']

    network = internal_test_client.create_network()
    subnet = internal_test_client.create_subnet(networkAddress='192.168.0.0',
                                                isPublic=True,
                                                cidrSize='16',
                                                networkId=network.id,
                                                startAddress='192.168.0.3',
                                                endAddress='192.168.0.3')
    subnet = internal_test_client.wait_success(subnet)
    assert subnet.state == 'active'

    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     subnetIds=[subnet.id])

    vm = internal_test_client.wait_success(vm)

    assert vm.state == 'running'
    assert vm.primaryIpAddress == '192.168.0.3'

    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     subnetIds=[subnet.id])
    vm = internal_test_client.wait_transitioning(vm)

    assert vm.state == 'removed'
    assert vm.transitioning == 'error'
    assert vm.transitioningMessage == \
        'Failed to allocate IP from subnet : IP allocation error'


def test_virtual_machine_stop_subnet(internal_test_client, sim_context, subnet,
                                     vnet):
    image_uuid = sim_context['imageUuid']

    vm = internal_test_client.create_virtual_machine(subnetIds=[subnet.id],
                                                     imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('192.168')

    vm = internal_test_client.wait_success(vm.stop())

    assert vm.state == 'stopped'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('192.168')
    assert nic.state == 'inactive'


def test_virtual_machine_remove_subnet(internal_test_client, sim_context,
                                       subnet, vnet):
    image_uuid = sim_context['imageUuid']

    vm = internal_test_client.create_virtual_machine(subnetIds=[subnet.id],
                                                     imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('192.168')

    vm = internal_test_client.wait_success(vm.stop(remove=True))

    assert vm.state == 'removed'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('192.168')
    assert nic.state == 'removed'


def test_virtual_machine_purge_subnet(internal_test_client, sim_context,
                                      subnet, vnet):
    image_uuid = sim_context['imageUuid']
    subnet_plain_id = get_plain_id(internal_test_client, subnet)
    vm = internal_test_client.create_virtual_machine(subnetIds=[subnet.id],
                                                     imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    addresses = internal_test_client.list_resource_pool(poolType='subnet',
                                                        poolId=subnet_plain_id)
    assert vm.primaryIpAddress in [x.item for x in addresses]
    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('192.168')

    vm = internal_test_client.wait_success(vm.stop(remove=True))

    assert vm.state == 'removed'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('192.168')
    assert nic.state == 'removed'

    vm = internal_test_client.wait_success(vm.purge())
    assert vm.state == 'purged'

    nics = vm.nics()
    assert len(nics) == 1

    nic = nics[0]
    assert nic.state == 'removed'
    assert nic.macAddress is not None

    nic = internal_test_client.wait_success(nic.purge())
    assert nic.state == 'purged'
    assert nic.macAddress is None

    assert len(nic.ipAddressNicMaps()) == 1
    assert nic.ipAddressNicMaps()[0].state == 'removed'
    assert len(nic.ipAddresses()) == 0

    ip_address = internal_test_client.reload(ip_address)
    assert ip_address.state == 'removed'
    assert ip_address.address is not None
    addresses = internal_test_client.list_resource_pool(poolType='subnet',
                                                        poolId=subnet_plain_id)
    assert vm.primaryIpAddress not in [x.item for x in addresses]


def test_virtual_machine_restore_subnet(internal_test_client, sim_context,
                                        subnet, vnet):
    image_uuid = sim_context['imageUuid']
    subnet_plain_id = get_plain_id(internal_test_client, subnet)
    vm = internal_test_client.create_virtual_machine(subnetIds=[subnet.id],
                                                     imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'running'

    addresses = internal_test_client.list_resource_pool(poolType='subnet',
                                                        poolId=subnet_plain_id)

    assert vm.primaryIpAddress in [x.item for x in addresses]
    vm = internal_test_client.wait_success(vm.stop())
    assert vm.state == 'stopped'

    vm = internal_test_client.wait_success(internal_test_client.delete(vm))
    assert vm.state == 'removed'

    assert vm.state == 'removed'
    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]
    address = ip_address.address
    assert ip_address.address.startswith('192.168')

    vm = vm.restore()
    assert vm.state == 'restoring'

    vm = internal_test_client.wait_success(vm)
    assert vm.state == 'stopped'

    assert len(vm.nics()) == 1
    nic = vm.nics()[0]
    assert nic.state == 'inactive'

    assert len(nic.ipAddresses()) == 1
    ip_address = nic.ipAddresses()[0]
    assert ip_address.state == 'active'

    vm = internal_test_client.wait_success(vm.start())

    assert vm.state == 'running'
    assert vm.nics()[0].ipAddresses()[0].address == address


def test_virtual_machine_console(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)

    assert vm.state == 'running'
    assert 'console' not in vm

    vm.data.fields['capabilities'] = ['console']
    vm = internal_test_client.update(vm, vm)

    assert 'console' in vm
    assert 'console' in vm and callable(vm.console)

    console = vm.console()

    assert console is not None
    assert console.kind == 'fake'
    assert console.url == 'http://localhost/console'


def test_virtual_machine_console_visibility(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid)
    vm = internal_test_client.wait_success(vm)

    assert vm.state == 'running'
    assert 'console' not in vm

    vm.data.fields['capabilities'] = ['console']
    vm = internal_test_client.update(vm, vm)

    assert 'console' in vm
    assert 'console' in vm and callable(vm.console)

    vm = internal_test_client.wait_success(vm.stop())

    assert vm.state == 'stopped'
    assert 'console' not in vm


def test_virtual_machine_account_defaults(internal_test_client, sim_context):
    image_uuid = sim_context['imageUuid']
    account = create_and_activate(internal_test_client, 'account',
                                  kind='user')
    cred = create_and_activate(internal_test_client, 'credential',
                               accountId=account.id)

    network = create_and_activate(internal_test_client, 'network')
    assert network.accountId != account.id
    network2 = create_and_activate(internal_test_client, 'network')
    assert network2.accountId != account.id

    account = internal_test_client.update(account,
                                          defaultCredentialIds=[cred.id],
                                          defaultNetworkIds=[network.id,
                                                             network2.id])
    assert account.state == 'active'
    assert account.defaultCredentialIds == [cred.id]
    assert account.defaultNetworkIds == [network.id, network2.id]

    vm = internal_test_client.create_virtual_machine(imageUuid=image_uuid,
                                                     accountId=account.id)
    vm = internal_test_client.wait_success(vm)

    assert vm.state == 'running'
    assert len(vm.credentials()) == 1
    assert vm.credentials()[0].id == cred.id

    network_ids = set([x.networkId for x in vm.nics()])
    assert network_ids == set([network.id, network2.id])
