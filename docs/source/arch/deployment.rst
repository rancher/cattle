Deployment Architecture
=======================

dStack can be deployed in many configurations to match the scale and needs of the deployed cloud.  The smallest configuration is a single process and the largest is a fully distributed set process with external system services such as MySQL, Redis, and Apache ZooKeeper.

Server Profiles
***************

dStack is distributed as a single jar file.  All the code needed to run dStack is in that single file.  When the JVM is launched you can specify a server profile that with determine if the JVM will run as all servers or specific one.  For example, if you run with :option:`-Dserver.profile=api-server` the JVM will only run as an API server.  Additionally, based on configuration you can tell dStack which system services to use such as Hazelcast or Apache ZooKeeper for locking.

Reconfiguring Environment
***********************************

If you start with the simplest deployment scenario (a single JVM) you can alway reconfigure the environment to more complex deployment architectures without requiring a reinstall.  This means you can start very simple and then you can adaptive to the needs of your cloud as things change. 

Example Deployment Scenarios
****************************

Single JVM
----------

.. image:: https://docs.google.com/drawings/d/1TWBTImxU-VEXGV7gPRuBMMcjc1OHHGukt9D1Cvdc5ls/pub?w=203&h=401
   :align: center

Great for just starting out or if you just want a simple step.  This setup will scale to meat the vast majority of dev/test clouds that exist today.

Redundant JVM
-------------

.. image:: https://docs.google.com/drawings/d/14odbJEFN_aZ0WmvA3owyKASx-0lXqYlesnkms-MRMM4/pub?w=473&h=490
   :align: center

In this scenario you'd like a bit of redundancy such that if a single process is down, the entire management stack is not down.  For this scenario we switch to an external database and then use Hazelcast to do locking and eventing.

Distributed services
--------------------

.. image:: https://docs.google.com/drawings/d/11qJq0QznsRZvMpHESHVBpqf7LsIF94lAO3XdvJtUudw/pub?w=800&h=572
   :align: center

In this scenario you'd like to break out the JVM components into distributed components such that you can scale and maintain them independently.  The API server, process server, and agent server all have different scaling characteristics.

The process server is used to throttle the concurrency and throughput of the entire system.  By adding more process serves you effectively 2x, 3x, 4x, etc the concurrency of the system.  More is not necessarily better.  The down stream systems such as the hypervisors and storage devices still run at a constant speed.  If you increase the concurrency such that downstream systems can not handle the work, you will be effectively wasting a lot of resources, because the majority of the process jobs will have to be rescheduled because the downstream system is busy.

The API server is scaled based on the amount of traffic from users.  If you have a significant amount of reads, for example, you may want to spin up some more API servers and point them to read slaves.  Or, imagine you have a UI.  There is a good reason to run API servers dedicated to the UI and then separate API servers dedicated to the public API.  If your public API gets hammered the UI will still be responsive (assuming that the public api is being properly throttled and not overwhelming the DB).

The number of agent servers are scaled in a ratio to match the number of hypervisors.  A persistent connection is maintained between the agent server and the agents to allow efficient management of the hypervisors.  As a result, you probably only want about 10,000 hypervisors per agent server.  An agent server can manage the connection of any agent in its assigned agent group.  Agent groups are used as a means of grouping and controlling the amount of connections managed by an agent server.
