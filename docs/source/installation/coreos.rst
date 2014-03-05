CoreOS Installation
===================

Installation on CoreOS is pretty straight forward.  You will basically will follow the :ref:`generic-install`.  

Run in Docker
*************
The key things to note is that you must run dStack in Docker, you're not going to install Java is CoreOS.  Specifically, you are going to be starting the dStack management server as follows

.. code-block:: bash

    docker run -d -p 8080:8080 ibuildthecloud/dstack

Adding a Server
***************
When registering a server you need to specify that the user will be this core and not the root user.

Don't put the SSH key in /home/core/.ssh/authorized_keys because that file may later be clobbered by update-ssh-keys used in CoreOS.  Instead use the update-ssh-keys script to register the key.  Additionally, when adding the agent you need specify the core user also.  Run the below commands instead of the commands in the :ref:`generic-install`.

.. code-block:: bash

    curl -s http://<HOST:PORT>/v1/authorized_keys | update-ssh-keys -A dstack
    curl -X POST http://<HOST:PORT>:8080/v1/agents -F user=core

Fleet
*****

Instructions for running dStack using fleet are on their way...
