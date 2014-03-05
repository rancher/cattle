#!/bin/bash
set -e

# This is just here to make sure your environment is sane
docker info

curl -s http://localhost:8080/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
curl -X POST http://localhost:8080/v1/agents
