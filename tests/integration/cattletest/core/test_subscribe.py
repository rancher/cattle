from common_fixtures import *  # NOQA

import websocket
import thread
import time

SUB_OPTIONS = '?include=hosts&include=instances&include=instanceLinks' \
              '&include=ipAddresses&eventNames=resource.change'


def test_websocket_close(client):
    def on_message(ws, message):
        print message

    def on_error(ws, error):
        print "### error: [%s] ###" % error

    def on_close(ws):
        print "### closed ###"

    def on_open(ws):
        print "### opened ###"

    websocket.enableTrace(True)

    subscribe_url = client.schema.types['subscribe'].links['collection']
    subscribe_url = subscribe_url.replace('http', 'ws') + SUB_OPTIONS
    subscribe_url = "ws://localhost:8080/v1/subscribe?eventNames=" \
                    "resource.change&include=hosts&include=instances" \
                    "&include=instance&include=loadBalancerConfig" \
                    "&include=loadBalancerTargets&include=" \
                    "loadBalancerListeners&include=instanceLinks" \
                    "&include=ipAddresses&projectId=1a5"
    assert subscribe_url is not None
    ws = websocket.WebSocketApp(subscribe_url,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)
    ws.on_open = on_open
    ws.run_forever()

    time.sleep(15)