#pyagent
This agent runs on compute nodes in a Rancher cluster. It receives events from the Rancher server, acts upon them, and returns response events.

### Deployment notes
This agent is typically deployed inside a container on Rancher compute nodes. See [the Rancher project](http://github.com/rancherio/rancher) for details.

### Setup and Develiopment notes
#### On Mac OS X
Steps to get the tests running and passing:

1. Have boot2docker up and running
1. Install libvirt and pkg-config (they're needed to cleanly install requirements.txt)
 
  ```
  $ brew install libvirt
  $ brew install pkg-config
  ```
1. Create virtual environment and install python dependencies:

  ```
  $ mkdir venv && virtualenv venv && . venv/bin/activate
  $ pip install -r requirements.txt
  $ pip install -r test-requirements.txt
  ```
1. Run the tests:

  ```
  mkdir $HOME/cattle-home
  $ CATTLE_DOCKER_USE_BOOT2DOCKER=true DOCKER_TEST=true CATTLE_HOME=$HOME/cattle-home \
  py.test tests
  ```
  Or you can do the equivalent in PyCharm. An explanation of those environment variables:
  * ```CATTLE_DOCKER_USE_BOOT2DOCKER=true``` tells the docker client to use the connection settings derived from ```boot2docker shellinit```. You need this because boot2docker has TLS enabled by default.
  * ```DOCKER_TEST=true``` tells the test framework to run the docker tests. They're disabled by default. We'll probably turn them on by default in the future.
  * ```CATTLE_HOME``` is needed for some temporary files that are written (locks, specifically)

