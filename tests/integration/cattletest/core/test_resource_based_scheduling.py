from common_fixtures import *  # NOQA
import websocket
import json
import requests
from test_healthcheck import _get_agent_client, _get_agent_for_container  # NOQA
from cattle import ClientApiError

SUB = '?eventNames=scheduler.prioritize' \
      '&eventNames=scheduler.reserve&eventNames=scheduler.release'


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
                       [{'resource': 'instanceReservation', 'amount': 1}])

    # Straight-forward memory scheduling
    do_scheduling_test({'imageUuid': image, 'memoryReservation': 500000,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host2, host3, host1], host2,
                       [{'resource': 'memoryReservation', 'amount': 500000},
                        {'resource': 'instanceReservation', 'amount': 1}])

    # Straight-forward cpu scheduling
    do_scheduling_test({'imageUuid': image, 'milliCpuReservation': 500,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host3], host3,
                       [{'resource': 'cpuReservation', 'amount': 500},
                        {'resource': 'instanceReservation', 'amount': 1}])

    # Two resources are requested
    do_scheduling_test({'imageUuid': image, 'memoryReservation': 5000000,
                        'milliCpuReservation': 500, 'networkMode': 'host'},
                       client, mock_scheduler,
                       [host1, host2, host3], host1,
                       [{'resource': 'memoryReservation', 'amount': 5000000},
                        {'resource': 'cpuReservation', 'amount': 500},
                        {'resource': 'instanceReservation', 'amount': 1}])

    # deactivate the host that the scheduler returns as #1 and the second
    # one in the list should get chosen
    host2 = client.wait_success(host2.deactivate())
    do_scheduling_test({'imageUuid': image, 'milliCpuReservation': 500,
                        'networkMode': 'host'},
                       client, mock_scheduler,
                       [host2, host3, host1], host3,
                       [{'resource': 'cpuReservation', 'amount': 500},
                        {'resource': 'instanceReservation', 'amount': 1}])

    do_no_hosts_match_test({'imageUuid': image, 'milliCpuReservation': 500,
                            'networkMode': 'host'},
                           client, mock_scheduler,
                           [{'resource': 'cpuReservation', 'amount': 500},
                            {'resource': 'instanceReservation', 'amount': 1}])


def do_scheduling_test(container_kw, client, mock_scheduler, hosts,
                       expected_host, expected_resource_requests):
    c = client.create_container(**container_kw)

    event = mock_scheduler.get_next_event()
    assert event['name'] == 'scheduler.prioritize'
    assert resource_reqs(event) == expected_resource_requests
    host_uuids = [host.uuid for host in hosts]
    data = {'prioritizedCandidates': host_uuids}
    mock_scheduler.publish(event, data)

    event = mock_scheduler.get_next_event()
    assert event['name'] == 'scheduler.reserve'

    assert host_id(event) == expected_host.uuid

    assert resource_reqs(event) == expected_resource_requests
    mock_scheduler.publish(event, {})

    c = client.wait_success(c)
    assert c.state == 'running'
    c = client.wait_success(c.stop())
    c = client.wait_success(c.remove())
    c.purge()

    event = mock_scheduler.get_next_event()
    assert event['name'] == 'scheduler.release'
    assert host_id(event) == expected_host.uuid
    assert resource_reqs(event) == expected_resource_requests


def do_no_hosts_match_test(container_kw, client, mock_scheduler,
                           expected_resource_requests):
    c = client.create_container(**container_kw)

    event = mock_scheduler.get_next_event()
    assert event['name'] == 'scheduler.prioritize'
    assert resource_reqs(event) == expected_resource_requests
    # Return empty candidate list
    data = {'prioritizedCandidates': []}
    mock_scheduler.publish(event, data)

    with pytest.raises(ClientApiError) as e:
        client.wait_success(c)
    assert e.value.message.startswith('Scheduling failed: No healthy hosts '
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
