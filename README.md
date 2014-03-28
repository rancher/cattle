# Cattle

**This is early alpha stuff.  You've been warned.**

Cattle is an infrastructure orchestration platform for such tasks as running containers and virtual machines.  It was created as a sandbox to try out new architectural ideas that would be either too risky or too difficult to do within an established platform like OpenStack or Apache CloudStack.

The focus of Cattle is to create a solid foundation and platform for orchestration.  The killer feature of Cattle is intended to be the strength of the platform.  The strength and flexibility of the platform should then be used to foster innovation and new ideas in the IaaS/PaaS space.

The key areas of focus are

* Simplicity
* Extensibility
* Scalability
* Reliability
* Operations

## What does it currently do?

As the focus is on the platform, much of the current effort has been on building a solid platform.  In terms of user functionality only the most basic Docker and libvirt operations are supported.

### Features

* Docker
    * Create
    * Start/Stop
    * Delete

* Libvirt
    * Create
    * Start/Stop
    * Delete

### Coming Next

A lot of networking features

# Getting Started

## Installing

Your best bet at the moment is to run on Ubuntu 13.10+.  Anything that runs Docker will eventually be supported, but development is done on Ubuntu 13.10.

### Management Server

Install Docker if you don't have it already, you can read the [official instructions][9] or just run

    curl -sL https://get.docker.io/ | sh

Start Cattle

    docker run -p 8080:8080 cattle/server


## Specific Installation Instructions

Below are installation instructions for specific environments.

[Boot2Docker][5] - Useful if you want to run docker on Mac OS X.

[CoreOS][6] - Useful if you want to run massively scalable infrastructure :)

## Generic Installation

For a simple installation you have two basic things that need to be setup, the management server and a hypervisor/Docker server.  They can be the same physical/virtual machine if you want.

<p align=center> ![Simple Architecture Picture][1]

### 1. Download

[cattle.jar (Main distribution)][3]

[cattle-scripts.tar.gz (Python API, CLI, and Samples)][4]

### 2. Management Server Installation

You can either run Cattle within docker or manually.  It is really a matter of preference which approach you take.  Running in docker has the advantage of not having to install Java and pretending that it doesn't exist.

#### 2a. The "Docker Way"

    docker run -p 8080:8080 cattle/cattle
    
**NOTE: This will use port 8080 on the host.  If port 8080 is not free, you can choose a different port by doing `-p 8081:8080` to use port 8081, for example.  Or if you want to just allocate a random port `-p 8080`.**

#### 2b. Manually

Java 6+ is required, Java 7+ is recommended.

    java -jar cattle.jar

### 3. Make sure it's running

It may take up to 30 seconds to startup the first time.  Once Cattle is running you should see the below message

> Startup Succeeded, Listening on port 8080

If you see tons of Java stack traces, then something bad happened.  If you can hit http://localhost:8080 and it serves up the UI you should be good to go.

**NOTE: If you are running the management server under Docker the listen port might not be 8080 but instead the random port that was chosen by Docker.  Run ```docker ps``` to see which port is mapped to 8080.**

### 4. Registering a Hypervisor/Docker Server

**Docker v0.8.0+ is required to be installed.**

From the hypervisor or Docker server run the following commands to register the node with management server replacing HOST:PORT with the host and port of the mangement server.  This is the same port that you used to access the UI.

```sh
curl http://<HOST:PORT>/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
curl -X POST http://<HOST:PORT>:8080/v1/agents
```

When Cattle first starts it generates a SSH key.  The above commands will download the public key and then register itself with the Cattle management server.  The default communication transport between the management server and the host is SSH.  There are other approaches, but SSH is the simplest and most secure approach for Linux based hosts.

### 5. Confirm registration worked

If you hit http://localhost:8080/v1/agents you should see one agent and the state should be "active."  If the state is not active within five minutes, then it probably didn't work.

If the agent is active go to http://localhost:8080/v1/hosts and within one minute you should see one host that is active.

### 6. Run something

There are some sample things you can do in Cattle in the [Documentation][7].  There are some samples for Docker too, if you not too familiar with it.

Or you can just open the UI to http://localhost:8080/v1/containers and click "Create."  Enter a imageUuid in the format "docker:name" for example "docker:cattle/cattle" or "docker:ubuntu" and a command, like "sleep 120."

## More Info

Documentation: http://cattle.readthedocs.org/en/latest/toc.html

### 7. Learn

If you've gotten this far, you have a basic setup of Cattle.  This setup will actually scale to hundreds of host and thousands of VMs just fine.  Basically >90% of existing clouds today will work with this simple setup.  There is the obvious downside that everything is running in a single process and there is no redundancy.  To learn how to do more complex setups with true redundancy and capable of scaling to the millions of things refer to the [Documentation][8].

# License
[Apache License, Version 2.0][2]

  [1]: https://docs.google.com/drawings/d/1M04-BY_cgeTEBGpf9uZ4YuOnL9jq438IB9uN0CAynOQ/pub?w=268&h=206
  [2]: http://www.apache.org/licenses/LICENSE-2.0.html
  [3]: https://github.com/cattleio/cattle/releases/download/v0.1-rc1/cattle.jar
  [4]: https://github.com/cattleio/cattle/releases/download/v0.1-rc1/cattle-scripts.tar.gz
  [5]: http://cattle.readthedocs.org/en/latest/installation/boot2docker.html
  [6]: http://cattle.readthedocs.org/en/latest/installation/coreos.html
  [7]: http://cattle.readthedocs.org/en/latest/examples/overview.html
  [8]: http://cattle.readthedocs.org/en/latest/toc.html
  [9]: http://docs.docker.io/en/latest/installation/
