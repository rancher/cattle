import time
import os
import logging
import signal
from threading import Thread
try:
    from subprocess32 import Popen
except:
    from subprocess import Popen

log = logging.getLogger('process-manager')


# There's nothing about this solution that I particularly like.  The basic
# problem is the agent will spawn many subprocesses that need to be alive as
# long as the agent is alive.  If the subprocess dies, it should be restarted.
# If the agent dies, the subprocess should die too.  A better solution could
# be to make the agent process be PID 1 in a new PID namespace.
class ProcessManager(object):
    def __init__(self):
        self.pids = {}

    def init(self):
        script = os.path.join(os.path.dirname(__file__), 'process_watcher.sh')

        launch_script = False
        try:
            # If this isn't UNIX, this will fail, so just ignore it
            os.setpgid(0, 0)
            signal.signal(signal.SIGCHLD, signal.SIG_IGN)
            launch_script = True
        except:
            pass

        if launch_script:
            self.background([script])

        t = Thread(target=self.watch)
        t.setDaemon(True)
        t.start()

    def watch(self):
        while True:
            time.sleep(2)
            for pid, spawn in self.pids.items():
                if not os.path.exists('/proc/{0}'.format(pid)):
                    self._exec(spawn, pid)

    def _exec(self, spawn, old_pid=None):
        try:
            new_pid = spawn()
            self.pids[new_pid] = spawn
            try:
                del self.pids[old_pid]
            except KeyError:
                pass
        except:
            log.exception('Failed to spawn process')

    def _exec_background(self, *args, **kw):
        log.info('Launching %s', args[0])
        p = Popen(*args, **kw)
        return p.pid

    def background(self, *args, **kw):
        self._exec(lambda: self._exec_background(*args, **kw))


_PROCESS_MANAGER = ProcessManager()

background = _PROCESS_MANAGER.background
init = _PROCESS_MANAGER.init
