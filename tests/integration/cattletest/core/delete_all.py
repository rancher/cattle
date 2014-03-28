#!/usr/bin/env python

import cattle

client = cattle.from_env()

l = client.list_container(removed_null=True)
while l is not None:
    for c in l:
        try:
            if c.state == 'stopped':
                print 'Deleting', c.id
                client.delete(c)
            else:
                print 'Stopping', c.id
                c.stop(remove=True)
        except:
            pass

    try:
        l = l.next()
    except:
        l = None
