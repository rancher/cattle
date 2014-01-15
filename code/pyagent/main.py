#!/usr/bin/env python2.7

import sys

from dstack.agent.agent import Agent

#sys.path.insert(0, "/home/darren/src/stack/pyagent/dstack/dist")

if __name__ == '__main__':
    agent = Agent()
    while True:
        line = sys.stdin.readline()
        print "Input:", line
        print agent.execute(line)
        sys.exit(0)
