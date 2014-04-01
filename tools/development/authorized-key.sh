#!/bin/bash
set -e

curl -s http://localhost:8080/v1/authorized_keys | sudo tee -a /root/.ssh/authorized_keys
