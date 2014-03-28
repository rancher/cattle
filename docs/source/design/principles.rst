Principles
==========

Simplicity
**********

Orchestrating infrastructure can be a relatively complex task.  There are a lot of moving parts and failures in systems are abundant and expected.  Cattle aims to make this complex problem simple.  To be frank, this is easier said than done.  It is not enough in an IaaS system to say that simplicity is at the level of the user interface (whether that be UI or API).  Simplicity must extend further down into the core of the system.  The fundamental truth of IaaS is that no two clouds are the same.  This is not a flaw or shorting coming of IaaS and should not be viewed as a "snowflake" problem.

Take the networking world as an example.  A vendor does not go to a customer and say every network should be exactly the same.  Instead they sell switches, routers, and other devices to assemble and create their own networks.  The reality though is that best practices and reference architectures arise.  But even when one implements a reference architecture, it is still based on a reference and variations will occur.  The compute and storage world have a similar story with reference architectures and variations.  IaaS aggregates networking, storage and compute into a single solution and thus the variations become multiplicative.  Overtime, reference IaaS architectures and best practices will emerge.

Cattle focuses on simple patterns in architecture and design in order to deal with the complex variations that will occur.  Consistently throughout the architecture of Cattle simple proven patterns are chosen over more complex patterns that are theoretically more complete.  The theorical solutions are left as a future optimization that may or may not be ever needed.  While simple and possibly non-comprehensive patterns are chosen it is not done naively.  All patterns are chosen knowing their limitations and what steps could be taken if those limitations become an issue.  

Scalability
***********

The scalability goals of Cattle are similar to most other IaaS stacks.  Cattle should scale to millions of devices and 100s of millions user resources (virtual machines) within a single control plane.  Billions and beyond will be possible with multiple federated control planes.  The difference with Cattle is that while that extreme scalability is important, we do not ignore the fact that the vast majority of clouds are less than 100 servers.

Aligned with the principle of simplicity, Cattle should be the easiest stack to run and should require a minimal amount of resources to run a cloud of a hundred servers.  A cloud of that size can be controlled by a laptop or a simple ARM device.

In order to meet high scalability requirements, particular attention is given to ensure that as devices, such as hypervisors, are added, that the resources they require are horizontally scalable.  For example, if a hypervisor holds a connection to a database, then that will not scale to millions because the database can not have that many connections.

Reliability
***********

Orchestration should never fail.  

The nature of infrastructure orchestration is that systems fail.  From a computer science perspective, systems fail quite often.  Additionally, their is a growing trend to build infrastructure on commodity hardware; commodity hardware often being synonymous with less reliable.

While the systems being orchestrated will fail, the orchestration itself should never fail.  This does not mean that all user requests will succeed.  If a server fails and their is not enough compute available, the request should result in an error.  The IaaS system will detect the insufficient capacity and systems failure and the user request will result in an error.  The orchestration succeeded, as the system did what it should do, but the operation resulted in a error.

To further expound on this point, orchestration should continue in the event of any system failure, whether that failed system is being orchestrated or it is part of the orchestration system itself.  This means if the orchestration management server dies while it is in the middle of doing an operation, that operation should continue if there is another management server available, or if not, when that server comes back online.

At the heart of Cattle, all architecture decisions are made with consideration to how one can reliability orchestrate knowing that all underlying systems are unreliable.
