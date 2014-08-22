#!/usr/bin/env python

import requests
import cattle


# Discovery Token
token = requests.get("https://discovery.etcd.io/new").text

userdata = """#cloud-config
coreos:
    units:
      - name: etcd.service
        command: start
      - name: fleet.service
        command: start
    etcd:
        discovery: %s
        addr: $private_ipv4:4001
        peer-addr: $private_ipv4:7001""" % token

client = cattle.from_env()

cred = client.list_ssh_key(uuid='defaultSshKey')[0]
image = client.list_image(uuid_like='coreos-stable-%')[0]

for i in range(3):
    c = client.create_virtual_machine(imageId=image.id,
                                      name='CoreOS {0}'.format(i+1),
                                      credentialIds=[cred.id],
                                      userdata=userdata,
                                      memoryMb=512)

    print 'Created Name: {}, ID: {}, Image: {}'.format(c.name,
                                                       c.id,
                                                       c.image().name)
