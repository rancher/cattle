from threading import Thread

from websocket import create_connection

from common_fixtures import *  # NOQA

SUB_OPTIONS = '?include=hosts&include=instances&include=instanceLinks' \
              '&include=ipAddresses&eventNames=resource.change'


def stop_collection(collector):
    collector()


def collect_events(client, array):
    should_stop = False

    def stop():
        global should_stop
        should_stop = True

    def collect():
        sub_url = client.schema.types['subscribe'].links['collection']
        assert sub_url is not None

        try:
            ws = create_connection(sub_url + SUB_OPTIONS,
                                   headers=auth_header(client))

            while True:
                line = ws.recv()
                if should_stop:
                    break

                if len(line.strip()):
                    # Should probably expose this functionality in gdapi
                    array.append(client._unmarshall(line))
        finally:
            if ws is not None:
                ws.close()

    t = Thread(target=collect)
    t.daemon = True
    t.start()

    return stop


def events_by_type(collected, type):
    return filter(lambda x: x.resourceType == type, collected)


@pytest.mark.skipif('True')
def test_events(admin_client, client, sim_context, request):
    collected = []
    collector = collect_events(admin_client, collected)
    request.addfinalizer(lambda: stop_collection(collector))

    wait_for(lambda: len(collected) > 0)

    c = admin_client.create_container(imageUuid=sim_context['imageUuid'],
                                      requestedHostId=sim_context['host'].id)
    c = admin_client.wait_success(c)

    assert len(collected) > 0
    wait_for(lambda: len(events_by_type(collected, 'instance')) > 0)

    for e in events_by_type(collected, 'instance'):
        assert len(e.data.resource.hosts) > 0
