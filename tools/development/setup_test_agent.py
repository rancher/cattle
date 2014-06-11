#!/usr/bin/env python

import cattle

def find_instance(instance):
    hosts = instance.hosts()
    if len(hosts) > 0:
        return hosts[0].agent().uuid == 'test-agent'
    return False

client = cattle.from_env()

UUID = 'docker0-agent-instance-provider'
nsp = client.list_network_service_provider(uuid=UUID)[0]
instances = filter(find_instance, nsp.instances())

if len(instances) != 1:
    raise Exception('Found {} instances, expect 1.  Try running a container'
                    'first'.format(len(instances)))

account = instances[0].agent().account()

found = False
for cred in account.credentials():
    if cred.kind == 'apiKey' and cred.publicValue == 'ai':
        found = True

if not found:
    print 'Creating credential for account', account.id
    client.create_credential(accountId=account.id,
                             publicValue='ai',
                             secretValue='aipass',
                             kind='apiKey')
