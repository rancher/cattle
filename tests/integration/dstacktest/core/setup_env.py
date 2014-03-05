#!/usr/bin/env python
from setup_env_common import *  # NOQA
import os

client = from_env('DSTACK')

purge_old_containers(client)

env = {
    'JAVA_OPTS': '-server -Xmx256m',
    'URL': 'http://demo.ibuildthecloud.com/dstack.war',
    #'URL': 'http://172.17.42.1/dstack.war',
}

if os.environ.get('DB_HOST') is not None:
    env['DSTACK_DB_DSTACK_DATABASE'] = 'mysql'
    env['DSTACK_DB_DSTACK_HOST'] = os.environ['DB_HOST']
    env['DSTACK_DB_DSTACK_MYSQL_HOST'] = os.environ['DB_HOST']
    env['DSTACK_DB_DSTACK_MYSQL_PORT'] = 3306
    env['DSTACK_DB_DSTACK_PASSWORD'] = os.environ['DB_PASS']

logstash = setup_logstash(client, env)
setup_graphite(client, env)
setup_kibana(logstash, client, env)
setup_h2(client, env)
setup_redis(client, env)
setup_zookeeper(client, env)

api_servers = create_servers(client, 3, env=env, profile='api-server')
process_servers = create_servers(client, 3, env=env, profile='process-server')
agent_servers_1 = create_servers(client, 3, env=env, profile='agent-server')

#setup_haproxy(client, api_servers)

print_servers(client)

for i in api_servers:
    print 'Api URL', get_http_url(wait_done(client, i), '8080/tcp')
