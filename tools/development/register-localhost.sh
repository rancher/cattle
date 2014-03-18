#!/bin/bash
set -e

# This is just here to make sure your environment is sane
docker info

curl -s http://localhost:8080/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
# Setting the uuid=test-agent is to make this not conflict with the integration tests.
# This was the automated test will use the same agent and not try to create a second one.
curl -X POST http://localhost:8080/v1/agents -F uuid=test-agent
