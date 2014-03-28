Quick Start Installation
========================

The installation consists of the two parts, the controller and at least one 
hypervisor.  It is possible that you use the same server as both as long as
it is supported as a hypervisor.

Controller Installation
^^^^^^^^^^^^^^^^^^^^^^^

Requirements

* Java 6+ Runtime
* Operating System: Anything Java runs on (Linux/OS X/Windows)

.. code-block:: bash

   wget http://corestack.io/corestack-latest.war
   java -jar corestack-latest.war

Should take about 5-10 seconds to start.  Open your browser to http://localhost:8080

Hypervisor
^^^^^^^^^^

The default hypervisors supported by Cattle are docker and KVM (really libvirt).

Ubuntu 12.04+
-------------


CentOS 6.5+
-----------
