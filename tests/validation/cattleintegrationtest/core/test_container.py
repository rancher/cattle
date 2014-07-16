from common_fixtures import *  # NOQA
import requests


def test_sibling_pinging(client, one_per_host):
    instances = one_per_host
    hosts = {}
    hostnames = set()

    for i in instances:
        port = i.ports()[0]

        host = port.publicIpAddress().address
        port = port.publicPort
        base_url = 'http://{}:{}'.format(host, port)
        pong = requests.get(base_url + '/ping').text
        hostname = requests.get(base_url + '/hostname').text

        assert pong == 'pong'
        assert hostname not in hostnames

        hostnames.add(hostname)
        hosts[hostname] = base_url

    count = 0
    for hostname, base_url in hosts.items():
        url = base_url + '/get'
        for other_hostname, other_url in hosts.items():
            if other_hostname == hostname:
                continue

            test_hostname = requests.get(url, params={
                'url': other_url + '/hostname'
            }).text

            count += 1
            assert other_hostname == test_hostname

    assert count == len(instances) * (len(instances) - 1)

    delete_all(client, instances)


def test_dynamic_port(client, test_name):
    c = client.create_container(name=test_name,
                                imageUuid=TEST_IMAGE_UUID)
    c = client.wait_success(c)

    ports = c.ports()
    assert len(ports) == 1

    port = ports[0]

    assert port.publicPort is None

    port = client.wait_success(client.update(port, publicPort=3001))

    assert port.publicPort == 3001
    ping_port(port)

    port = client.wait_success(client.update(port, publicPort=3002))

    assert port.publicPort == 3002
    ping_port(port)

    delete_all(client, [c])


def test_linking(client, test_name):
    hosts = client.list_host(kind='docker', removed_null=True)
    assert len(hosts) > 2

    random = random_str()
    random2 = random_str()

    link_server = client.create_container(name=test_name + '-server',
                                          imageUuid=TEST_IMAGE_UUID,
                                          hostname=test_name + '-server',
                                          environment={
                                              'VALUE': random
                                          },
                                          requestedHostId=hosts[2].id)
    link_server2 = client.create_container(name=test_name + '-server2',
                                           imageUuid=TEST_IMAGE_UUID,
                                           hostname=test_name + '-server2',
                                           environment={
                                               'VALUE': random2
                                           },
                                           requestedHostId=hosts[1].id)
    link_client = client.create_container(name=test_name + '-client',
                                          imageUuid=TEST_IMAGE_UUID,
                                          ports=['3000:3000'],
                                          hostname=test_name + '-client1',
                                          instanceLinks={
                                              'client': link_server.id
                                          },
                                          requestedHostId=hosts[0].id)

    link_client = client.wait_success(link_client)
    link_server = client.wait_success(link_server)

    ping_link(link_client, 'client', var='VALUE', value=random)

    link_server2 = client.wait_success(link_server2)

    link = link_client.instanceLinks()[0]
    link = client.update(link, targetInstanceId=link_server2.id)
    client.wait_success(link)

    ping_link(link_client, 'client', var='VALUE', value=random2)

    delete_all(client, [link_client, link_server, link_server2])
