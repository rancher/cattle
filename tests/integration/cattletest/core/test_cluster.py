from common_fixtures import *  # NOQA


def test_create_cluster(admin_client):
    cluster = admin_client.create_cluster(
        name='testcluster1', port=9000, discoverySpec='file:///tmp/my_cluster')

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    assert_fields(cluster, {
        "kind": "cluster",
        "port": 9000,
        "discoverySpec": "file:///tmp/my_cluster",
        "state": "active"
    })


def test_create_cluster_with_default_discovery_spec(admin_client):
    cluster = admin_client.create_cluster(
        name='testcluster2', port=9001)

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    assert_fields(cluster, {
        "port": 9001,
        "state": "active"
    })
    # Uncomment after mock external handler is put in place
    # assert cluster.discoverySpec is not None
    # assert str(cluster.discoverySpec).startswith('token://')


def test_cluster_add_remove_host_actions(admin_client, super_client):
    cluster = admin_client.create_cluster(
        name='testcluster3', port=9000)

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    host1 = super_client.create_host()
    host1 = super_client.wait_success(host1)

    cluster = cluster.addhost(hostId=str(host1.id))

    assert len(cluster.hosts()) == 1
    assert cluster.hosts()[0].id == host1.id
    assert len(host1.clusters()) == 1
    assert host1.clusters()[0].id == cluster.id

    try:
        cluster.addhost(hostId=str(host1.id))
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    cluster = cluster.removehost(hostId=str(host1.id))

    # TODO: Create a more generic wait function taking in a function
    start = time.time()
    while len(cluster.hosts()) != 0:
        if time.time() - start > 10:
            raise Exception(
                'Timeout waiting for host to be remove'
                )

        time.sleep(.5)

    assert len(cluster.hosts()) == 0

    try:
        cluster = cluster.removehost(hostId=str(host1.id))
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    cluster = cluster.addhost(hostId=str(host1.id))
    assert len(cluster.hosts()) == 1

    host2 = super_client.create_host()
    host2 = super_client.wait_success(host2)

    cluster = cluster.addhost(hostId=str(host2.id))

    assert len(cluster.hosts()) == 2


def test_host_purge(admin_client, super_client):
    cluster = admin_client.create_cluster(
        name='testcluster5', port=9000)

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    host1 = super_client.create_host()
    host1 = super_client.wait_success(host1)

    cluster = cluster.addhost(hostId=str(host1.id))
    host1 = admin_client.wait_success(host1.deactivate())
    host1 = admin_client.wait_success(admin_client.delete(host1))
    host1 = admin_client.wait_success(host1.purge())

    assert len(cluster.hosts()) == 0


def test_cluster_purge(admin_client, super_client):
    cluster = admin_client.create_cluster(
        name='testcluster5', port=9000)

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    host1 = super_client.create_host()
    host1 = super_client.wait_success(host1)

    cluster = cluster.addhost(hostId=str(host1.id))

    cluster = admin_client.wait_success(cluster.deactivate())
    cluster = admin_client.wait_success(admin_client.delete(cluster))
    cluster = admin_client.wait_success(cluster.purge())

    assert len(cluster.hosts()) == 0


def test_cluster_actions_invalid_host_ref(admin_client, super_client):
    cluster = admin_client.create_cluster(
        name='testcluster5', port=9000)

    cluster = wait_until_expected_state(admin_client, cluster, 'active')

    try:
        cluster.addhost(hostId='badvalue')
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'

    try:
        cluster.removehost(hostId='badvalue')
        assert False
    except cattle.ApiError as e:
        assert e.error.code == 'InvalidReference'
