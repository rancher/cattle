Vision
======

Using the analogy of a Cloud Operation System, Cattle would be the kernel. Xen, KVM, Docker, Ceph would be the drivers.  EC2, GCE, OpenStack, and CloudStack APIs would be glibc.  User space would be the plethora of tools and applications that consume Cloud APIs.

.. image:: https://docs.google.com/drawings/d/1IepAyBX3nct2hSP-y4ErwrQsVa9pVdviCeuSpc9fdtc/pub?w=604&h=260
   :align: right

Cattle is first and foremost a lightweight, flexible, and reliable orchestration engine.  The umbrella of Cattle is purposely intended to not reach too far.  The goal of the platform is to stay focuses on building the core building blocks needed to assemble a full scale IaaS solution.  As one moves up the stack in functionality, by design, it should be less and less specific to Cattle.  

As one writes a simple server side application today, whether they are running on Linux, Solaris, or FreeBSD is largely inconsequential.  As long as your runtime, such as Python, Node.js, or Ruby, is supported, the details of the underlying technology are often abstracted away.  As such is the vision of Cattle.  While it is design to run the largest and most complex clouds that can exist, it is intended that most application will be abstracted away from Cattle by either an API such as EC2 or a platform such as CloudFoundry.

Cattle aims to fit into a larger ecosystem of cloud and infrastructure tools that already exist today.  The intention is not to build a new ecosystem around Cattle, but instead be a member of a larger ecosystem by leveraging the standards (official or de-facto) that already exist.  With this in mind, being architecturally compatible with APIs such as EC2 is a top priority.
