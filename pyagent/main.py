#!/usr/bin/env python2.7

from dstack.agent import Agent
import sys
sys.path.insert(0, "/home/darren/src/cloudstack/pyagent/dstack/dist")

if __name__ == '__main__':
    agent = Agent()
    print agent.execute("""
        { "name" : "storage.stage.template", "data" : { "storagePool" : {}, "template" : { "name" : "ubuntu" } } }
    """)
#    while True:
#        line = sys.stdin.readline()
#        print agent.execute(line)
