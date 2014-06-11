import logging
import os

from threading import Thread
from subprocess import Popen, PIPE
from cattle.type_manager import get_type, MARSHALLER
from cattle import Config
from cattle.utils import get_command_output

log = logging.getLogger('docker')


_NET_UTIL = os.path.join(os.path.dirname(__file__), 'net-util.sh')


def pipe_error(ctx, stderr, buffer):
    for line in stderr:
        if len(line) > 0:
            buffer.append(line)
            log.error('[%s] %s', ctx, line[:-1])


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

    output = get_command_output(sudo(args))
    log.info(output)


def sudo(args):
    if os.getuid() == 0:
        return args
    return ['sudo'] + args


def add_to_env(config, *args, **kw):
    try:
        env = config['environment']
    except KeyError:
        env = {}
        config['environment'] = env

    for i in range(0, len(args), 2):
        if args[i] not in env:
            env[args[i]] = args[i+1]

    for k, v in kw.items():
        if k not in env:
            env[k] = v
