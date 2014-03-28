Logical System Architecture
===========================

Overview

.. image:: https://docs.google.com/drawings/d/1c-j3i8WmdjOR4-K7vED7fzTfdkVMVe2RJoYoQDSMgT8/pub?w=800&h=572
   :align: center

Cattle system architecture is separated into the following logical servers.

Database
********
Cattle is based on a RDBMS.  The currently support databases are MySQL, H2 (Java), and HSQLDB (Java).  Other databases such as PostgreSQL and SQL Server will be supported in the future.  Cattle is built upon jOOQ and Liquibase to abstract the database.  Any database that is supported by both of these frameworks should eventually be supported by Cattle.  Since the abstraction provided by these libraries is not perfect, the limiting factor in adopting other databases is the required testing.

Lock Manager
************

A distributed lock manager (DLM) is used to control concurrent access to resources.  Cattle currently supported Hazelcast and Zookeeper as a lock manager.

Event Bus
*********

Communication between distributed components is done using an event bus.  The messaging style used in Cattle can be best compared to UDP multicast.  There is no assumptions of reliability or persistence in the messaging transport.  This means that from an operational standpoint the messaging system is not considered a repository of state.  If you were to do maintenance one could completely shutdown, delete, and reinstall the messaging servers with no impact except that operations will pause until the messaging system is back online.  The currently supported messaging systems are Redis and Hazelcast.

API Server
**********

The API server accepts the HTTP API requests from the user.  This server is based on a Java Servlet 3.0 container.  By default Cattle ships with Jetty 8 embedded, but it is also possible to deploy Cattle as a standard war on any Servlet Container such as Tomcat, WebLogic, or WebSphere.

.. note::
  The current dashboard UI uses WebSockets and the current Cattle WebSocket implementation is Jetty specific.  This is concidered a flaw and should be fixed to work on any servlet container, or the dashboard should support line terminated event polling.

The API server only needs the database and lock manager to be available to service requests.  This means for maintanence all other back-end services can be completely stopped if necessary.  The API server is stateless and does not do any orchestration logic.  All orchestration logic is deferred to the back-end servers.  Once a user receives a 201 or 202 HTTP response, the remaining logic will be preformed elsewhere.

Process server
**************

The process server can be viewed as the main loop of the system.  It is responsible for controlling the execution of processes.  If the process server is stopped no requests will be executed until it is brought back online.

Agent Server
************
The agent server is responsible for the communication to and from the remote agents.  Remote agents have no access to the database, the event bus, or lock manager.  The agent server presents a HTTP REST API that the remote agents can use to publish and subscribe to the event bus.  This proxy serves two purposes.  First, it is used as a security mechanism to control which events are accessible to the remote agent.  Second, it is used as a means of scaling the cloud.  When scaling number of hosts, much of the scaling concerns have to do with persistent socket connections.  By putting an intermediate server between the agent and the event bus, this makes scaling the hosts move of a factor of O(n^2) and not O(n).

Remote Agents
*************

Remote agents can be developed in any language.  The default agent is developed in python and intended to run on a Linux host.  Agents are designed to be dumb.  They do not initiate an action by themselves and have no ability to call out and get information.  When a command is sent to an agent all of the data that is needed for the action is encapsulated in the command.  The agent then performs the request and returns the result.  All operations are done in a request, reply fashion, with all requests initiated by the core orchestration system and never the agent.
