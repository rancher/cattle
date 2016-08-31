from cattle import ApiError

from common_fixtures import *  # NOQA
from test_shared_volumes import add_storage_pool


def _create_virtual_machine(client, context, **kw):
    args = {
        'accountId': context.project.id,
        'imageUuid': context.image_uuid,
    }
    args.update(kw)

    return client.create_virtual_machine(**args)


@pytest.fixture(scope='module')
def network(context):
    return context.nsp.network()


@pytest.fixture(scope='module')
def subnet(network):
    return network.subnets()[0]


def test_virtual_machine_case_sensitivity(super_client, client, context):
    name = random_str()
    volume = client.create_volume(name='R' + name, driver='local')

    assert volume.name == 'R' + name
    assert volume.state == 'requested'

    disks = [
        {
            'name': 'r' + name,
            'size': '2g',
        },
    ]

    vm = _create_virtual_machine(client, context, name=random_str(),
                                 volumeDriver='foo-bar',
                                 userdata='hi', vcpu=2, memoryMb=42,
                                 disks=disks)
    vm = client.wait_success(vm)
    assert vm.state == 'running'

    c = super_client.reload(vm)
    assert len(c.dataVolumes) == 3
    n = '{}-{}-r{}:/volumes/disk00'.format(vm.name, vm.uuid[0:7], name)
    assert c.dataVolumes[0] == '/var/lib/rancher/vm:/vm'
    assert c.dataVolumes[1] == '/var/run/rancher:/var/run/rancher'
    assert c.dataVolumes[2] in {'R{}:/volumes/disk00'.format(name), n}


test_disks = [
    {
        'size': '2g',
    },
    {
        'name': 'foo',
        'size': '2g',
        'root': True,
    },
    {
        'name': 'nope',
        'size': '2g',
        'root': True,
    },
    {
        'size': '2g',
    },
]


def test_virtual_machine_with_device_enable_storage_pool(super_client, client,
                                                         context):
    sp = add_storage_pool(context, [context.host.uuid],
                          block_device_path="/dev/test")
    sp_name = sp.name
    vm = _create_virtual_machine(client, context, name=random_str(),
                                 volumeDriver=sp_name,
                                 userdata='hi', vcpu=2, memoryMb=42,
                                 disks=test_disks)
    vm = client.wait_success(vm)
    assert vm.state == 'running'

    c = super_client.reload(vm)
    prefix = c.name + '-' + c.uuid[0:7]
    assert c.devices == ['/dev/kvm:/dev/kvm',
                         '/dev/net/tun:/dev/net/tun',
                         '/dev/test/{}-00:/dev/vm/disk00'.format(prefix),
                         '/dev/test/{}-foo:/dev/vm/root'.format(prefix),
                         '/dev/test/{}-01:/dev/vm/disk01'.format(prefix)]

    volume1 = find_one(client.list_volume, name=prefix + '-foo')
    assert volume1.driver == sp_name
    assert volume1.driverOpts == {'vm': 'true',
                                  'base-image': context.image_uuid,
                                  'dont-format': 'true'}


def test_virtual_machine_root_disk(super_client, client, context):
    vm = _create_virtual_machine(client, context, name=random_str(),
                                 volumeDriver='foo-bar',
                                 userdata='hi', vcpu=2, memoryMb=42,
                                 disks=test_disks)
    vm = client.wait_success(vm)
    assert vm.state == 'running'

    c = super_client.reload(vm)
    prefix = c.name + '-' + c.uuid[0:7]
    assert c.dataVolumes == ['/var/lib/rancher/vm:/vm',
                             '/var/run/rancher:/var/run/rancher',
                             '{}-00:/volumes/disk00'.format(prefix),
                             '{}-foo:/image'.format(prefix),
                             '{}-01:/volumes/disk01'.format(prefix)]
    assert len(c.devices) == 2


def test_virtual_machine_default_fields(super_client, client, context):
    disk_name = 'disk' + random_str()
    disks = [
        {
            'size': '2g',
            'readIops': 10000,
            'writeIops': 9000,
            'opts': {
                'foo': 'bar'
            }
        },
        {
            'name': disk_name,
            'driver': 'foo',
        }
    ]

    vm = _create_virtual_machine(client, context,
                                 volumeDriver='foo-bar',
                                 userdata='hi', vcpu=2, memoryMb=42,
                                 disks=disks)
    vm = client.wait_success(vm)
    assert vm.state == 'running'
    assert vm.vcpu == 2
    assert vm.memoryMb == 42

    c = super_client.reload(vm)

    assert c.labels['io.rancher.vm'] == 'true'
    assert c.labels['io.rancher.vm.memory'] == '42'
    assert c.labels['io.rancher.vm.vcpu'] == '2'
    assert c.labels['io.rancher.vm.userdata'] == 'hi'
    assert c.dataVolumes == ['/var/lib/rancher/vm:/vm',
                             '/var/run/rancher:/var/run/rancher',
                             '{}-00:/volumes/disk00'.format(c.uuid[0:7]),
                             '{}-{}:/volumes/disk01'.format(c.uuid[0:7],
                                                            disk_name)]
    assert c.devices == ['/dev/kvm:/dev/kvm', '/dev/net/tun:/dev/net/tun']
    assert c.capAdd == ['NET_ADMIN']
    assert c.capabilities == ['console']

    volume1 = find_one(client.list_volume, name=c.uuid[0:7] + '-00')
    assert volume1.driver == 'foo-bar'
    assert volume1.driverOpts == {'vm': 'true', 'size': '2g', 'foo': 'bar',
                                  'read-iops': '10000', 'write-iops': '9000'}

    x = c.uuid[0:7] + '-' + disk_name
    volume2 = find_one(client.list_volume, name=x)
    assert volume2.name == x
    assert volume2.driver == 'foo'
    assert volume2.driverOpts == {'vm': 'true', 'size': '40g'}

    assert c.dataVolumeMounts == {
        '/volumes/disk00': volume1.id,
        '/volumes/disk01': volume2.id,
    }


def test_virtual_machine_stats(client, context):
    vm = _create_virtual_machine(client, context, vcpu=2, memoryMb=42)
    vm = client.wait_success(vm)
    assert vm.state == 'running'

    assert 'stats' in vm
    assert 'containerStats' in vm


def test_virtual_machine_create_cpu_memory(client, context):
    vm = _create_virtual_machine(client, context,
                                 vcpu=2, memoryMb=42)

    vm = client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu == 2
    assert vm.memoryMb == 42


def test_virtual_machine_create(super_client, context):
    vm = _create_virtual_machine(super_client, context)

    vm = super_client.wait_success(vm)
    assert vm.state == 'running'

    assert vm.vcpu == 1
    assert vm.memoryMb == 512


def test_virtual_machine_create_null_network_id(super_client, context):
    image_uuid = context.image_uuid
    try:
        super_client.create_virtual_machine(imageUuid=image_uuid,
                                            networkIds=[None])
        assert False
    except ApiError as e:
        assert e.error.code == 'NotNullable'


def test_virtual_machine_n_ids_s_ids(super_client, context,
                                     network, subnet):
    image_uuid = context.image_uuid
    try:
        super_client.create_virtual_machine(imageUuid=image_uuid,
                                            networkIds=[network.id],
                                            subnetIds=[subnet.id])
    except ApiError as e:
        assert e.error.code == 'NetworkIdsSubnetIdsMutuallyExclusive'


def test_virtual_machine_network(super_client, context, network, subnet):
    subnet_plain_id = get_plain_id(super_client, subnet)
    vm = _create_virtual_machine(super_client, context,
                                 networkIds=[network.id])

    vm = super_client.wait_success(vm)
    assert vm.state == 'running'
    assert 'networkIds' not in vm

    nics = vm.nics()
    assert len(nics) == 1

    nic = nics[0]

    assert nic.network().id == network.id
    assert nic.state == 'active'
    assert nic.macAddress is not None
    assert nic.macAddress.startswith(network.macPrefix)

    nic_admin = super_client.reload(nic)
    vm_admin = super_client.reload(vm)

    assert nic_admin.account().id == vm_admin.accountId

    ips = nic.ipAddresses()

    assert len(ips) == 1
    assert super_client.reload(nic).ipAddressNicMaps()[0].state == 'active'

    ip = ips[0]
    ip_admin = super_client.reload(ip)

    assert ip_admin.account().id == vm_admin.accountId
    assert ip_admin.subnet().id == nic_admin.subnet().id
    assert ip_admin.role == 'primary'

    assert ip.address is not None
    assert ip.address.startswith('10.42')

    assert vm.primaryIpAddress is not None
    assert vm.primaryIpAddress == ip.address

    addresses = super_client.list_resource_pool(poolType='subnet',
                                                poolId=subnet_plain_id)
    assert ip.address in [x.item for x in addresses]


def test_virtual_machine_subnet(super_client, context, subnet):
    network = subnet.network()
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])

    vm = super_client.wait_success(vm)
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
    assert ip.address.startswith('10.42')

    assert vm.primaryIpAddress is not None
    assert vm.primaryIpAddress == ip.address


def test_virtual_machine_no_ip(super_client, context):
    account_id = context.project.id
    network = super_client.create_network(accountId=account_id)
    subnet = super_client.create_subnet(networkAddress='192.168.0.0',
                                        accountId=account_id,
                                        cidrSize='16',
                                        networkId=network.id,
                                        startAddress='192.168.0.3',
                                        endAddress='192.168.0.3')
    subnet = super_client.wait_success(subnet)
    assert subnet.state == 'active'
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])

    vm = super_client.wait_success(vm)

    assert vm.state == 'running'
    assert vm.primaryIpAddress == '192.168.0.3'

    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])
    vm = super_client.wait_transitioning(vm)

    assert vm.state == 'error'
    assert vm.transitioning == 'error'
    assert vm.transitioningMessage == \
        'Failed to allocate IP from subnet : IP allocation error'


def test_virtual_machine_stop_subnet(super_client, context, subnet):
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])
    vm = super_client.wait_success(vm)
    assert vm.state == 'running'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('10.42')

    vm = super_client.wait_success(vm.stop())

    assert vm.state == 'stopped'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('10.42')
    assert nic.state == 'inactive'


def test_virtual_machine_remove_subnet(super_client, context, subnet):
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])
    vm = super_client.wait_success(vm)
    assert vm.state == 'running'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('10.42')

    vm = super_client.wait_success(vm.stop(remove=True))

    assert vm.state == 'removed'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('10.42')
    assert nic.state == 'removed'


def test_virtual_machine_purge_subnet(super_client, context, subnet):
    subnet_plain_id = get_plain_id(super_client, subnet)
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])
    vm = super_client.wait_success(vm)
    assert vm.state == 'running'

    addresses = super_client.list_resource_pool(poolType='subnet',
                                                poolId=subnet_plain_id)
    assert vm.primaryIpAddress in [x.item for x in addresses]
    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1
    assert vm.nics()[0].ipAddresses()[0].address.startswith('10.42')

    vm = super_client.wait_success(vm.stop(remove=True))

    assert vm.state == 'removed'

    assert len(vm.nics()) == 1
    assert len(vm.nics()[0].ipAddresses()) == 1

    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]

    assert ip_address.state == 'active'
    assert ip_address.address.startswith('10.42')
    assert nic.state == 'removed'

    vm = super_client.wait_success(vm.purge())
    assert vm.state == 'purged'

    nics = vm.nics()
    assert len(nics) == 1

    nic = nics[0]
    assert nic.state == 'removed'
    assert nic.macAddress is not None

    nic = super_client.wait_success(nic.purge())
    assert nic.state == 'purged'
    assert nic.macAddress is None

    assert len(nic.ipAddressNicMaps()) == 1
    assert nic.ipAddressNicMaps()[0].state == 'removed'
    assert len(nic.ipAddresses()) == 0

    ip_address = super_client.reload(ip_address)
    assert ip_address.state == 'removed'
    assert ip_address.address is not None
    addresses = super_client.list_resource_pool(poolType='subnet',
                                                poolId=subnet_plain_id)
    assert vm.primaryIpAddress not in [x.item for x in addresses]


def test_virtual_machine_restore_subnet(super_client, context, subnet):
    subnet_plain_id = get_plain_id(super_client, subnet)
    vm = _create_virtual_machine(super_client, context,
                                 subnetIds=[subnet.id])
    vm = super_client.wait_success(vm)
    assert vm.state == 'running'

    addresses = super_client.list_resource_pool(poolType='subnet',
                                                poolId=subnet_plain_id)

    assert vm.primaryIpAddress in [x.item for x in addresses]
    vm = super_client.wait_success(vm.stop())
    assert vm.state == 'stopped'

    vm = super_client.wait_success(super_client.delete(vm))
    assert vm.state == 'removed'

    assert vm.state == 'removed'
    nic = vm.nics()[0]
    ip_address = nic.ipAddresses()[0]
    address = ip_address.address
    assert ip_address.address.startswith('10.42')

    vm = vm.restore()
    assert vm.state == 'restoring'

    vm = super_client.wait_success(vm)
    assert vm.state == 'stopped'

    assert len(vm.nics()) == 1
    nic = vm.nics()[0]
    assert nic.state == 'inactive'

    assert len(nic.ipAddresses()) == 1
    ip_address = nic.ipAddresses()[0]
    assert ip_address.state == 'active'

    vm = super_client.wait_success(vm.start())

    assert vm.state == 'running'
    assert vm.nics()[0].ipAddresses()[0].address == address


def test_virtual_machine_console(super_client, context):
    vm = _create_virtual_machine(super_client, context)
    vm = super_client.wait_success(vm)

    assert 'console' in vm
    assert 'console' in vm and callable(vm.console)

    console = vm.console()

    assert console is not None
    assert console.url.endswith('/v1/console/')


def test_virtual_machine_console_visibility(super_client, context):
    vm = _create_virtual_machine(super_client, context)
    vm = super_client.wait_success(vm)

    assert 'console' in vm
    assert 'console' in vm and callable(vm.console)

    vm = super_client.wait_success(vm.stop())

    assert vm.state == 'stopped'
    assert 'console' not in vm
