from common_fixtures import *  # NOQA


def test_create_network_driver_create_delete(client, super_client):
    driver_name = 'test' + random_str()
    stack = client.create_stack(name=random_str())
    super_client.update(stack, system=True)
    s = client.create_service(name=random_str(),
                              stackId=stack.id,
                              metadata={
                                  'network_driver': {
                                      'name': driver_name,
                                      'kind': 'cni',
                                  }
                              })

    s = client.wait_success(s)
    assert s.state == 'inactive'

    nds = client.list_network_driver(serviceId=s.id,
                                     name=driver_name)
    assert len(nds) == 0

    s = client.wait_success(s.activate())
    assert s.state == 'active'

    nd = find_one(client.list_network_driver, serviceId=s.id, name=driver_name)
    nd = client.wait_success(nd)

    assert nd.state == 'active'
    assert nd.kind == 'cni'
    assert nd.serviceId == s.id

    s = client.wait_success(s.remove())
    assert s.state == 'removed'
    nd = client.wait_success(nd)
    assert nd.state == 'removed'
