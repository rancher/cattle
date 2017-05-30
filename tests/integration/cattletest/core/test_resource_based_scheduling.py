from common_fixtures import *  # NOQA
import websocket
import json
import requests
from test_healthcheck import _get_agent_client, _get_agent_for_container  # NOQA
from cattle import ClientApiError

SUB = '?eventNames=scheduler.prioritize' \
      '&eventNames=scheduler.reserve&eventNames=scheduler.release'


@pytest.mark.skipif('True')
def test_resource_based_scheduler(new_context, super_client):
    """
    This tests cattle's side of the 'external resource-based scheduler.
    It creates a mock external scheduler that subscribes and responds to
    scheduling events.

    So, the tests will create containers with resource constraints and assert
    that the appropriate events are received by the mock scheduler.

    It will also send different responses to the events and assert that cattle
    responds appropriately.

    This single test has several sub-tests because setting up and managing the
    mock scheduler is expensive and we only want to do it once.

    """

    host1 = new_context.host
    host2 = register_simulated_host(new_context)
    host3 = register_simulated_host(new_context)

    mock_scheduler = mock_sched(new_context, super_client)
    image = new_context.image_uuid
    client = new_context.client

    # No explicit resrequested, but an instanceReservation is still made
    do_scheduling_test({'imageUuid': image, 'networkMode': 'host'},
                       client, mock_scheduler,
                       [host2, host3, host1], host2,
                       [{'resource': 'instanceReservation', 'amount': 1,
                         'type': 'computePool'}], super_client)

    # Straight-forward memory scheduling
    do_scheduling_test({'imageUuid': image, 'memoryReservation': 500000,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host2, host3, host1], host2,
                       [{'resource': 'memoryReservation', 'amount': 500000,
                         'type': 'computePool'},
                        {'resource': 'instanceReservation', 'amount': 1,
                         'type': 'computePool'}], super_client)

    # Straight-forward cpu scheduling
    do_scheduling_test({'imageUuid': image, 'milliCpuReservation': 500,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host3], host3,
                       [{'resource': 'cpuReservation', 'amount': 500,
                         'type': 'computePool'},
                        {'resource': 'instanceReservation', 'amount': 1,
                         'type': 'computePool'}], super_client)

    # Two resources are requested
    do_scheduling_test({'imageUuid': image, 'memoryReservation': 5000000,
                        'milliCpuReservation': 500, 'networkMode': 'host'},
                       client, mock_scheduler,
                       [host1, host2, host3], host1,
                       [{'resource': 'memoryReservation', 'amount': 5000000,
                         'type': 'computePool'},
                        {'resource': 'cpuReservation',
                            'amount': 500, 'type': 'computePool'},
                        {'resource': 'instanceReservation', 'amount': 1,
                         'type': 'computePool'}], super_client)

    # deactivate the host that the scheduler returns as #1 and the second
    # one in the list should get chosen
    host2 = client.wait_success(host2.deactivate())
    do_scheduling_test({'imageUuid': image, 'milliCpuReservation': 500,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host2, host3, host1], host3,
                       [{'resource': 'cpuReservation', 'amount': 500,
                         'type': 'computePool'},
                        {'resource': 'instanceReservation', 'amount': 1,
                         'type': 'computePool'}], super_client)

    do_no_hosts_match_test({'imageUuid': image, 'milliCpuReservation': 500,
                            'networkMode': 'host'},
                           client, mock_scheduler, [host1],
                           [{'resource': 'cpuReservation', 'amount': 500,
                             'type': 'computePool'},
                            {'resource': 'instanceReservation', 'amount': 1,
                             'type': 'computePool'}])


def test_delete_host(new_context, super_client):
    host = new_context.host

    mock_sched(new_context, super_client)
    image = new_context.image_uuid
    client = new_context.client

    agent = super_client.reload(host).agent()
    super_client.update(agent, state='reconnecting')

    register_simulated_host(new_context)
    c = client.create_container({'imageUuid': image, 'networkMode': 'host'})
    c = client.wait_success(c)
    assert c.state == 'running'


def do_scheduling_test(container_kw, client, mock_scheduler, hosts,
                       expected_host, expected_resource_requests,
                       super_client):
    c = client.create_container(**container_kw)

    event = None
    host_uuids = [host.uuid for host in hosts]
    data = {'prioritizedCandidates': host_uuids}

    # Iterate through and respond to events until we get one for the container
    # being tested. Looking for the prioritize event.
    while True:
        event = mock_scheduler.get_next_event()
        mock_scheduler.publish(event, data)
        if event['resourceId'] == c.id:
            break
    assert event['name'] == 'scheduler.prioritize'
    assert resource_reqs(event) == expected_resource_requests

    def check():
        ihms = super_client.reload(c).instanceHostMaps()
        if len(ihms) == 0:
            return false
        return ihms[0].state == 'active'
    # Looking for reserve event
    while True:
        event = mock_scheduler.get_next_event()
        mock_scheduler.publish(event, data)
        if (event['resourceId'] == c.id and
                event['name'] == 'scheduler.reserve'):
            try:
                # We have to do this because when idempotency checks are on,
                # the same reserve request will be sent multiple times and it
                # isnt safe to move on until a instanceHostMap has been created
                wait_for(check, timeout=1)
                break
            except:
                pass

    assert host_id(event) == expected_host.uuid
    assert resource_reqs(event) == expected_resource_requests

    c = client.wait_success(c)
    assert c.state == 'running'
    client.delete(c)

    def stop_check():
        return client.reload(c).removed is not None

    while True:
        event = mock_scheduler.get_next_event()
        mock_scheduler.publish(event, data)
        # Look away now
        res_id = event['resourceId']
        res_id = res_id.split('iir')[1] if 'iir' in res_id else res_id
        if (res_id == c.id.split('i')[1] and
                event['name'] == 'scheduler.release'):
            try:
                wait_for(stop_check, timeout=1)
                break
            except:
                pass

    c = client.wait_success(c)

    # Looking for release event
    while True:
        event = mock_scheduler.get_next_event()
        mock_scheduler.publish(event, data)
        # Look away now
        res_id = event['resourceId']
        res_id = res_id.split('iir')[1] if 'iir' in res_id else res_id
        if res_id == c.id.split('i')[1]:
            break
    assert event['name'] == 'scheduler.release'
    assert host_id(event) == expected_host.uuid
    assert resource_reqs(event) == expected_resource_requests


def do_no_hosts_match_test(container_kw, client, mock_scheduler, hosts,
                           expected_resource_requests):
    c = client.create_container(**container_kw)

    matched = {'prioritizedCandidates': hosts}
    # Point of this test is to send back no host for the container under test
    not_matched = {'prioritizedCandidates': []}
    done = False
    event = None
    while True:
        event = mock_scheduler.get_next_event()
        if event['resourceId'] == c.id:
            data = not_matched
            done = True
        else:
            data = matched
        mock_scheduler.publish(event, data)
        if done:
            break
    assert event['name'] == 'scheduler.prioritize'
    assert resource_reqs(event) == expected_resource_requests

    with pytest.raises(ClientApiError) as e:
        client.wait_success(c)
    assert e.value.message.startswith('Allocation failed: No healthy hosts '
                                      'meet the resource constraints')


def host_id(event):
    return event['data']['schedulerRequest']['hostID']


def resource_reqs(event):
    return event['data']['schedulerRequest']['resourceRequests']


def mock_sched(new_context, super_client):
    labels = {
        'io.rancher.container.create_agent': 'true',
        'io.rancher.container.agent_service.scheduling': 'true',
    }
    c = new_context.create_container(name='scheduler-agent',
                                     healthCheck={'port': 80},
                                     labels=labels)
    super_client.update(c, healthState='healthy')

    # Get agent credentials
    c = super_client.reload(c)
    agent = c.agent()
    account = agent.account()
    creds = filter(lambda x: x.kind == 'agentApiKey', account.credentials())
    access_key = creds[0].publicValue
    secret_key = creds[0].secretValue

    url = new_context.client.schema.types['subscribe'].links['collection']
    subscribe_url = url.replace('http', 'ws') + SUB
    ws = websocket.create_connection(subscribe_url,
                                     header=auth_header_from_keys(access_key,
                                                                  secret_key))

    publish_url = url.replace('subscribe', 'publish')
    scheduler = MockScheduler(ws, publish_url, (access_key, secret_key))
    scheduler.new_context = new_context

    return scheduler


class MockScheduler(object):

    def __init__(self, ws, url, auth):
        self._url = url
        self._auth = auth
        self._session = requests.Session()
        self.ws = ws

    def publish(self, event, resp_data):
        resp = {
            'name': event['replyTo'],
            'data': resp_data,
            'resourceType': event['resourceType'],
            'resourceId': event['resourceId'],
            'previousIds': [event['id']],
            'previousNames': [event['name']],
        }
        line = json.dumps(resp)
        r = self._session.post(self._url, data=line, auth=self._auth,
                               timeout=60)
        if r.status_code != 201:
            assert False, "Error [%s], Request [%s]" % (r.text, line)

    def get_next_event(self):
        while True:
            msg = self.ws.recv()
            event = json.loads(msg)
            if event['name'] != 'ping':
                return event
