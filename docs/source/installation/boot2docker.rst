boot2docker Installation
========================

Install boot2docker
*******************

Refer to https://github.com/boot2docker/boot2docker for instructions to install boot2docker.

The super short instructions for installing boot2docker on OS X are:

.. code-block:: bash

    brew update
    brew install boot2docker
    boot2docker init
    boot2docker up

Log into boot2docker
********************

Log into boot2docker with :command:`boot2docker ssh -L 8080:localhost:8080`.  This will forward port 8080 from your laptop/desktop to the VirtualBox VM.  boot2docker does not handle setting up port forwards, so adding :command:`-L 8080:localhost:8080` is essential if you wish to access the API/UI of Cattle.

.. code-block:: bash

    boot2docker ssh -L 8080:localhost:8080

**The password is tcuser**

Start Cattle
************

.. code-block:: bash

    docker run -d -p 8080:8080 cattle/server

Setup SSH Keys
**************

.. code-block:: bash

    mkdir -p /home/docker/.ssh
    chmod 700 /home/docker/.ssh
    curl -s http://localhost:8080/v1/authorized_keys | tee -a /home/docker/.ssh/authorized_keys

Register Server
***************

.. code-block:: bash

    curl -X POST http://localhost:8080/v1/agents -F user=docker

It may take a couple minutes to register the server because the server must pull a special image to run the agent under.

Check Status
************

If something doesn't seem to be working you can look at the logs with :command:`docker logs -f CONTAINER_ID`.  If everything worked correctly you should see a host named boot2docker at http://localhost:8080/static/.
