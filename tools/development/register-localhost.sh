#!/bin/bash
set -e

DEV_HOST=${DEV_HOST:-localhost:8080}

# This is just here to make sure your environment is sane
docker info

curl -s http://${DEV_HOST}/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys

# Setting the uuid=test-agent is to make this not conflict with the integration tests.
# This was the automated test will use the same agent and not try to create a second one.
curl -X POST http://${DEV_HOST}/v1/agents -F uuid=test-agent
