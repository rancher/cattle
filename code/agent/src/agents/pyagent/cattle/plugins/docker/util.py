import logging
import os

from threading import Thread
from subprocess import Popen, PIPE
from cattle.type_manager import get_type, MARSHALLER
from cattle import Config
from cattle.utils import get_command_output

log = logging.getLogger('docker')


_local_bin = os.path.join(os.path.dirname(__file__), 'nsenter')
if os.path.exists(_local_bin):
    _NSENTER = _local_bin
else:
    _NSENTER = 'nsenter'

_NET_UTIL = os.path.join(os.path.dirname(__file__), 'net-util.sh')


def pipe_error(ctx, stderr):
    for line in stderr:
        if len(line) > 0:
            log.error('[%s] %s', ctx, line[:-1])


def container_exec(pid, event):
    marshaller = get_type(MARSHALLER)
    input = marshaller.to_string(event)

    cmd = '{0}/events/{1}'.format(Config.agent_instance_cattle_home(),
                                  event.name)
    output = ns_exec(event.name, pid, input, cmd)

    if output is None:
        return None
    else:
        return marshaller.from_string(output)


def net_util(pid, ip=None, mac=None, device=None):
    args = [_NET_UTIL, '-p', str(pid)]

    if ip is not None:
        args.append('-i')
        args.append(ip)

    if mac is not None:
        args.append('-m')
        args.append(mac)

    if device is not None:
        args.append('-d')
        args.append(device)

    output = get_command_output(args)
    log.info(output)


def ns_exec(ctx, pid, input, *args, **kw):
    cmd = [_NSENTER, '-m', '-u', '-i', '-n', '-p', '-t', str(pid),
           '--']
    cmd.extend(args)
    p = Popen(cmd, stdin=PIPE, stderr=PIPE, stdout=PIPE)

    Thread(target=pipe_error, args=(ctx, p.stderr)).start()

    p.stdin.write(input)
    p.stdin.close()

    data = None
    # TODO Timeouts on read and wait
    for line in p.stdout:
        if len(line) > 0:
            if line.startswith('{'):
                data = line
            else:
                log.info('[%s] %s', ctx, line[:-1])

    p.wait()

    if p.returncode != 0:
        raise Exception('Exit code [{0}]'.format(p.returncode))

    return data
