.. _generic-install:

Detailed Installation
=====================

These instructions are a bit more granular than :ref:`generic-install` but allow you to install and run Cattle in contexts where Docker does not exist.

For a simple installation you have two basic things that need to be setup, the management server and a hypervisor/Docker server.  They can be the same physical/virtual machine if you want.

.. image:: https://docs.google.com/drawings/d/1M04-BY_cgeTEBGpf9uZ4YuOnL9jq438IB9uN0CAynOQ/pub?w=268&h=206
   :align: center

1. Download
***********

If you are installing using Docker, you do not need to download anything.  If you are just running Java manually, then you must download the :file:`cattle.jar` from the `Releases Page <https://github.com/rancherio/cattle/releases>`_

2. Management Server Installation
*********************************

You can either run Cattle within docker or manually.  It is really a matter of preference which approach you take.  Running in docker has the advantage of not having to install Java and pretending that it doesn't exist.

2a. The "Docker Way"
--------------------

.. code-block:: bash

    docker run -p 8080:8080 cattle/server
    
**NOTE: This will use port 8080 on the host.  If port 8080 is not free, you can choose a different port by doing `-p 8081:8080` to use port 8081, for example.  Or if you want to just allocate a random port `-p 8080`.**

2b. Manually
------------

Java 6+ is required, Java 7+ is recommended.

.. code-block:: bash

    java -jar cattle.jar

3. Make sure it's running
*************************

It may take up to 30 seconds to startup the first time.  Once Cattle is running you should see the below message

    Startup Succeeded, Listening on port 8080

If you see tons of Java stack traces, then something bad happened.  If you can hit http://localhost:8080 and it serves up the UI you should be good to go.

**NOTE: If you are running the management server under Docker the listen port might not be 8080 but instead the random port that was chosen by Docker.  Run "docker ps" to see which port is mapped to 8080.**

4. Registering a Hypervisor/Docker Server
*****************************************

**Docker v0.9.1+ is required to be installed.**

From the hypervisor or Docker server run the following commands to register the node with management server replacing HOST:PORT with the host and port of the mangement server.  This is the same port that you used to access the UI.

.. code-block:: bash

    curl http://<HOST:PORT>/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
    curl -X POST http://<HOST:PORT>:8080/v1/agents

When Cattle first starts it generates a SSH key.  The above commands will download the public key and then register itself with the Cattle management server.  The default communication transport between the management server and the host is SSH.  There are other approaches, but SSH is the simplest and most secure approach for Linux based hosts.

5. Confirm registration worked
******************************

If you hit http://localhost:8080/v1/agents you should see one agent and the state should be "active."  If the state is not active within five minutes, then it probably didn't work.

If the agent is active go to http://localhost:8080/v1/hosts and within one minute you should see one host that is active.

6. Run something
****************

You can now open the UI to http://localhost:8080/v1/containers and click "Create."  Enter a imageUuid in the format "docker:name" for example "docker:cattle/server" or "docker:ubuntu" and a command, like "sleep 120."
