.. _dstack-example:

dStack Example
==============

To get a basic overview of how Docker in dStack works we will use dStack to deploy a simple multi-tiered application.  This is the same application that we :ref:`deployed with Docker <docker-example>`.

.. image:: https://docs.google.com/drawings/d/1jXfAGAb2h0oYGZlRh-ihFWGp2bPvI4M2drOaV3ESrHc/pub?w=402&h=219
   :align: center

The application will we be deploying is dStack itself and additionally log aggregation with Logstash/Elasticsearch and Kibana for the UI.  The full script to run all of the below commands is `dstack-scripts.zip download <https://github.com/ibuildthecloud/dstack/tree/master#2-download>`_ in dstack-example.py.

**NOTE: dStack doesn't currently have native support for links but the semantics of links can be easily mimiced on the client side.  The link() function referenced in the following sections is a small python function to emulate the functionality of Docker links across hosts.  The source of the link() function is in the common.py file.**

For the following example we will be using the Python API to access dStack.

Setup Client
************

Create an instance of the dStack API client pointing to the API URL.

.. code-block:: python

    import dstack
    client = dstack.from_env(url='http://localhost:8080')

Change url to point to your actual API URL.

Launch Logstash/Elastisearch
****************************

We're going to launch logstash with Elasticsearch embedded.

.. code-block:: python

    logstash = client.create_container(name='logstash', imageUuid='docker:ibuildthecloud/logstash')

Notice that the imageUuid starts with "docker:"  This is very important.  The "docker:" prefix indicates to dStack that that image is expected to be a docker image and it will check the public index for it.

Launch Kibana
*************

We will now launch Kibana and link it to the embedded Elasticsearch.

.. code-block:: python

    kibana = client.create_container(name='kibana',
                                     imageUuid='docker:ibuildthecloud/kibana',
                                     environment=link(es=logstash))

You can run hit http://localhost:8080/v1/containers?name=kibana and you should see JSON similar to below.  dockerHostIp refers to the IP of the server and then refer to dockerPorts to see what port got exposed as

.. code-block:: javascript

    {
        "id": "1i54",
        "type": "container",
        "links": { … },
        "actions": { … },
        "accountId": "1a1",
        "allocationState": "active",
        "compute": null,
        "created": "2014-03-04T22:27:42Z",
        "createdTS": 1393972062000,
        "data": { … },
        "description": null,
        "hostname": null,
        "imageId": "1i8",
        "imageUuid": "docker:ibuildthecloud/kibana",
        "kind": "container",
        "name": "kibana",
        "removeTime": null,
        "removed": null,
        "requestedHostId": null,
        "startOnCreate": true,
        "state": "running",
        "transitioning": "no",
        "transitioningMessage": null,
        "transitioningProgress": null,
        "uuid": "77adc9a5-27dd-44be-b2b5-a0e1d4c9c5a2",
        "environment": {
            "ES_PORT_9200_TCP": "tcp://192.168.3.143:49156",
            "ES_PORT": "udp://192.168.3.143:49153",
            "ES_PORT_9200_TCP_PROTO": "tcp",
            "ES_PORT_12201_UDP_PORT": "49153",
            "ES_PORT_9200_TCP_ADDR": "192.168.3.143",
            "ES_PORT_12201_UDP_ADDR": "192.168.3.143",
            "ES_PORT_9200_TCP_PORT": "49156",
            "ES_PORT_12201_UDP": "udp://192.168.3.143:49153",
            "ES_NAME": "/self/es",
            "ES_PORT_12201_UDP_PROTO": "udp",
        },
        "command": null,
        "commandArgs": null,
        "directory": null,
        "user": null,
        "tcpPorts": null,
        "udpPorts": null,
        "dockerPorts": {
            "80/tcp": "49159",
        },
        "dockerHostIp": "192.168.3.143",
        "dockerIp": "172.17.0.8",
    }

You can use gist https://gist.github.com/ibuildthecloud/5d0ae8b4dd408ff8181a as a sample dashboard.

.. image:: kibana-gist.png
   :align: center
   :width: 25%

Launch MySQL
************

.. code-block:: python

    mysql = client.create_container(name='mysql',
                                    imageUuid='docker:ibuildthecloud/mysql')

You can now hit http://localhost:8080/v1/containers?name=mysql to see which port phpMyAdmin is running on.  Hit http://localhost:PORT/phpmyadmin and you can login with dstack/dstack for the account.

Launch dStack
*************

Now launch dStack linking it to MySQL and Logstash (using GELF).

.. code-block:: python

    client.create_container(name='dstack',
                                 imageUuid='docker:ibuildthecloud/dstack',
                                 environment=link(mysql=db,
                                                  gelf=logstash))

You can now hit http://localhost:8080/v1/containers?name=dstack to see what port dStack is running on.
