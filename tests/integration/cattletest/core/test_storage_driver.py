from common_fixtures import *  # NOQA
from cattle import ApiError


class Context:
    def __init__(self, ctx, driver):
        self.new_context = ctx
        self.driver = driver


@pytest.fixture(scope='function')
def storage_driver_context(new_context, super_client):
    client = new_context.client
    stack = client.create_stack(name=random_str(),
                                startOnCreate=True)
    super_client.update(stack, system=True)
    stack = client.wait_success(stack)
    assert stack.state == 'active'

    s = client.create_storage_driver_service(
        name=random_str(),
        startOnCreate=True,
        stackId=stack.id,
        storageDriver={
            'foo': 'bar'
        })
    s = client.wait_success(s)
    assert s.state == 'active'
    assert s.kind == 'storageDriverService'
    wait_for(lambda: len(s.storageDrivers()) == 1)
    driver = find_one(s.storageDrivers)
    return Context(new_context, driver)


def test_storage_driver_in_use(new_context, super_client):
    client = new_context.client
    stack = client.create_stack(name=random_str(),
                                startOnCreate=True)
    super_client.update(stack, system=True)
    stack = client.wait_success(stack)
    assert stack.state == 'active'

    s = client.create_storage_driver_service(
        name=random_str(),
        startOnCreate=True,
        stackId=stack.id,
        storageDriver={
            'foo': 'bar'
        })
    s = client.wait_success(s)
    assert s.state == 'active'

    driver = find_one(s.storageDrivers)
    vol_name = random_str()
    c = new_context.create_container(dataVolumes=[
                                        '{}:/tmp'.format(vol_name)
                                    ],
                                    volumeDriver=driver.name)
    c = client.wait_success(c)
    assert c.state == 'running'

    vol = find_one(client.list_volume, name=vol_name)
    assert find_one(vol.storagePools).storageDriverId == driver.id

    with pytest.raises(ApiError):
        s.deactivate()

    with pytest.raises(ApiError):
        client.delete(s)

    with pytest.raises(ApiError):
        stack.deactivateservices()

    with pytest.raises(ApiError):
        client.delete(stack)


def test_create_storage_driver_create_delete(new_context, super_client):
    client = new_context.client
    host = new_context.host

    assert len(host.storagePools()) == 1

    driver_name = 'test' + random_str()
    stack = client.create_stack(name=random_str())
    super_client.update(stack, system=True)
    s = client.create_storage_driver_service(
        name=random_str(),
        stackId=stack.id,
        storageDriver={
            'name': driver_name,
            'volumeAccessMode': driver_name,
            'blockDevicePath': 'some path',
            'volumeCapabilities': [
                'superAwesome',
            ],
        })

    s = client.wait_success(s)
    assert s.state == 'inactive'

    sds = client.list_storage_driver(serviceId=s.id,
                                     name=driver_name)
    assert len(sds) == 1

    s = client.wait_success(s.activate())
    assert s.state == 'active'

    sd = find_one(client.list_storage_driver, serviceId=s.id, name=driver_name)
    sd = client.wait_success(sd)

    find_one(s.storageDrivers)

    assert sd.state == 'active'
    assert sd.kind == 'storageDriver'
    assert sd.serviceId == s.id
    assert sd.scope == 'environment'
    assert sd.volumeAccessMode == 'multiHostRW'

    pools = [x for x in host.storagePools() if x.storageDriverId == sd.id]
    assert len(host.storagePools()) == 2
    assert len(pools) == 1

    stack = client.wait_success(stack.remove())
    assert stack.state == 'removed'

    s = client.wait_success(s)
    assert s.state == 'removed'
    sd = client.wait_success(sd)
    assert sd.state == 'removed'


def test_volume_create_from_driver(storage_driver_context):
    client = storage_driver_context.new_context.client
    host = storage_driver_context.new_context.host
    driver = storage_driver_context.driver

    volume = client.create_volume(name=random_str(),
                                  driver=driver.name,
                                  hostId=host.id)
    volume = client.wait_success(volume)
    assert volume.state == 'detached'
    assert volume.storageDriverId == driver.id
    assert volume.driver == driver.name


def test_volume_create_from_driver2(storage_driver_context, super_client):
    client = storage_driver_context.new_context.client
    host = storage_driver_context.new_context.host
    driver = storage_driver_context.driver

    volume = client.create_volume(name=random_str(),
                                  storageDriverId=driver.id,
                                  hostId=host.id)
    volume = client.wait_success(volume)
    assert volume.state == 'detached'
    assert volume.storageDriverId == driver.id
    assert volume.driver == driver.name

    volume = super_client.reload(volume)
    assert len(volume.storagePools()) == 1

    volume = client.wait_success(volume.remove())
    assert volume.removed is not None


def test_volume_create_from_user(storage_driver_context):
    client = storage_driver_context.new_context.client
    host = storage_driver_context.new_context.host
    driver = storage_driver_context.driver

    volume = client.create_volume(name=random_str(),
                                  storageDriverId=driver.id)
    volume = client.wait_success(volume)
    assert volume.state == 'inactive'
    assert volume.storageDriverId == driver.id
    assert volume.driver == driver.name

    volume = client.update(volume, hostId=host.id)
    volume = client.wait_success(volume)
    assert volume.state == 'detached'
    assert volume.hostId == host.id
