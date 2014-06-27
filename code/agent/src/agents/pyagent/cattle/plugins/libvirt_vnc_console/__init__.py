import time
import os
import logging
import random
from threading import Thread

from cattle import Config
from cattle.agent.handler import BaseHandler
from cattle.plugins.libvirt.config import LibvirtConfig
from cattle.plugins.libvirt import enabled
from cattle.type_manager import register_type, LIFECYCLE, REQUEST_HANDLER
from cattle.concurrency import spawn

log = logging.getLogger('libvirt')


class WebsockifyProxy(BaseHandler):
    def __init__(self):
        self.compute = LibvirtCompute()
        pass

    def events(self):
        return ['console.access']

    def _check_supports(self, req):
        try:
            return req.data.host.kind == 'libvirt'
        except:
            return False

    def console_access(self, req=None, instance=None, host=None, **kw):
        host, port, password = self.compute.get_vnc_connection_info(instance,
                                                                    host)

        if host is None:
            return self._reply(req, None)

        id = '%030x' % random.randrange(16**30)
        session_file = os.path.join(LibvirtConfig.websockify_session_dir(),
                                    id)

        with open(session_file, 'w') as f:
            log.info('Creating websockify session %s %s:%s', id, host, port)
            f.write('{0}: {1}:{2}\n'.format(id, host, port))

        host = LibvirtConfig.websockify_listen_host()
        port = LibvirtConfig.websockify_listen_port()
        path = 'websockify?token={0}'.format(id)
        return self._reply(req, {
            'url': 'ws://{0}:{1}/{2}'.format(host, port, path),
            'host': host,
            'port': port,
            'path': path,
            'kind': 'websocket-vnc',
            'password': password
        })

    def cleanup(self):
        while True:
            try:
                time.sleep(60)
                self._do_cleanup()
            except:
                log.exception('Exception while reaping sessions')

    def _do_cleanup(self):
        sessions = LibvirtConfig.websockify_session_dir()
        timeout = LibvirtConfig.websockify_session_timeout()

        if not os.path.exists(sessions):
            return

        for child in [os.path.join(sessions, x) for x in os.listdir(sessions)]:
            if time.time() - os.path.getmtime(child) > timeout:
                log.info('Removing websockify session %s', child)
                os.remove(child)

    def on_startup(self):
        sessions = LibvirtConfig.websockify_session_dir()

        if not os.path.exists(sessions):
            os.makedirs(sessions)

        def run_vnc():
            while True:
                try:
                    port = LibvirtConfig.websockify_listen_port()
                    host = LibvirtConfig.websockify_listen_host()
                    opts = {
                        'RequestHandlerClass': RequestHandler,
                        'target_cfg': sessions,
                        'listen_host': host,
                        'listen_port': port
                    }

                    log.info('Launching Websockify listening on %s:%s',
                             host, port)

                    server = websockify.WebSocketProxy(**opts)
                    server.start_server()
                except:
                    log.exception('Failed to launch Websockify')

                time.sleep(2)

        spawn(target=run_vnc, args=[])

        self._do_cleanup()

        t = Thread(target=self.cleanup)
        t.setDaemon(True)
        t.start()

        LibvirtConfig.set_console_enabled(True)


if enabled():
    try:
        import websockify
        from websockify import ProxyRequestHandler
        from cattle.plugins.libvirt.compute import LibvirtCompute
        ws = WebsockifyProxy()
        register_type(LIFECYCLE, ws)
        register_type(REQUEST_HANDLER, ws)

        class RequestHandler(ProxyRequestHandler):
            def new_websocket_client(self):
                if Config.is_eventlet():
                    from eventlet import hubs
                    hubs.use_hub()

                return ProxyRequestHandler.new_websocket_client(self)
    except:
        log.exception('Not starting websocket proxy for VNC')
