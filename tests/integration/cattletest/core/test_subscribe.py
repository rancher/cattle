from common_fixtures import *  # NOQA

import websocket
import time

SUB_OPTIONS = '?include=hosts&include=instances&include=instanceLinks' \
              '&include=ipAddresses&eventNames=resource.change&projectId=%s'


def test_websocket_close(client, context):
    assertions = {}

    def on_message(ws, message):
        assertions['messaged'] = True
        ws.close()

    def on_error(ws, error):
        assert False, "Got an error: %s" % error

    def on_close(ws):
        assertions['closed'] = True

    def on_open(ws):
        assertions['opened'] = True

    websocket.enableTrace(True)

    subscribe_url = client.schema.types['subscribe'].links['collection']
    options = SUB_OPTIONS % context.project.id
    subscribe_url = subscribe_url.replace('http', 'ws') + options
    ws = websocket.WebSocketApp(subscribe_url,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close,
                                on_open=on_open)
    ws.run_forever()
    time.sleep(.5)
    assert assertions['closed']
    assert assertions['opened']
    assert assertions['messaged']
