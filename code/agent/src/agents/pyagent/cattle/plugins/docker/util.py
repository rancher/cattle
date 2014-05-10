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


def pipe_error(ctx, stderr, buffer):
    for line in stderr:
        if len(line) > 0:
            buffer.append(line)
            log.error('[%s] %s', ctx, line[:-1])


def container_exec(pid, event):
    marshaller = get_type(MARSHALLER)
    input = marshaller.to_string(event)

    cmd = '{0}/events/{1}'.format(Config.agent_instance_cattle_home(),
                                  event.name)
    exit_code, output, data = ns_exec(event.name, pid, input, cmd)

    if data is not None:
        data = marshaller.from_string(data)

    return exit_code, output, data


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


def ns_exec(ctx, pid, input, *args, **kw):
    cmd = [_NSENTER, '-m', '-u', '-i', '-n', '-p', '-t', str(pid),
           '--']
    cmd.extend(args)
    p = Popen(sudo(cmd), stdin=PIPE, stderr=PIPE, stdout=PIPE, env={})

    output = []
    data = None

    Thread(target=pipe_error, args=(ctx, p.stderr, output)).start()
    try:
        p.stdin.write(input)
        p.stdin.close()
    except Exception:
        log.exception('Error calling %s', cmd)

    # TODO Timeouts on read and wait
    for line in p.stdout:
        if len(line) > 0:
            if line.startswith('{'):
                data = line
            else:
                log.info('[%s] %s', ctx, line[:-1])
                output.append(line)

    p.wait()

    return p.returncode, ''.join(output), data


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
