#!/usr/bin/env python

from cattle import from_env
import time


def _time_str(start, stop, ms):
    running = ''
    if stop is None:
        running = '*'
        stop = int(time.time() * 1000)

    duration = stop - start
    if ms:
        unit = 'ms'
    else:
        duration = duration/1000
        unit = 'seconds'

    return '{}{} {}'.format(running, duration, unit)


def print_time(pi):
    if 'stopTime' in pi:
        return _time_str(pi.startTime, pi.stopTime, True)

    if 'endTimeTS' not in pi or pi.endTimeTS is None:
        return _time_str(pi.startTimeTS, None, False)

    return _time_str(pi.startTimeTS, pi.endTimeTS, False)


def is_running(pi):
    if 'stopTime' in pi:
        return pi.stopTime is None

    return 'endTimeTS' not in pi or pi.endTimeTS is None


def print_pi(pi, detail=False):
    print print_time(pi), \
        pi.id, \
        pi.processName, \
        '{}:{}'.format(pi.resourceType, pi.resourceId), \
        pi.phase, \
        pi.exitReason, \
        pi.result

    if detail or is_running(pi):
        for pe in pi.processExecutions():
            for x in pe.log.executions:
                print_pe(x, prefix=' ')


def print_pe(pe, prefix=''):
    print prefix, print_time(pe), 'PROCESS:', pe.name, \
        '{}:{}'.format(pe.resourceType, pe.resourceId), pe.exitReason
    for phe in pe.processHandlerExecutions:
        print_phe(phe, prefix=prefix + '  ')


def print_phe(phe, prefix=''):
    print prefix, print_time(phe), 'HANDLER:', phe.name
    for child in phe.children:
        for pe in child.executions:
            print_pe(pe, prefix=prefix + '  ')


if __name__ == '__main__':
    import sys
    client = from_env(headers={'X-API-Project-Id': 'USER'})
    if len(sys.argv) == 1:
        for pi in client.list_process_instance(sort='startTime', order='desc',
                                               endTime_null=True,
                                               limit=1000):
            print_pi(pi)
    else:
        pi = client.by_id_process_instance(sys.argv[1])
        print_pi(pi, detail=True)
