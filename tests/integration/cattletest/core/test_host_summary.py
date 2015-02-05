from common_fixtures import *  # NOQA


def test_host_summary_list(client,
                           super_client,
                           user_sim_context,
                           user_sim_context2,
                           user_sim_context3,
                           user_account):


    no_ip_host_context = create_sim_context(super_client, 'simagent-noip',
                                            account=user_account)
    assert len(no_ip_host_context['host'].ipAddresses()) == 0

    hosts = {}
    contexts = [user_sim_context, user_sim_context2, user_sim_context3,
                no_ip_host_context]

    for i in contexts:
        hosts[i['host'].id] = i['host']

    containers = []
    for i in range(4):
        host = contexts[i]['host']
        for j in range(i*2):
            c = client.create_container(imageUuid='sim:',
                                        requestedHostId=host.id)
            containers.append(c)
            if j % 2 == 0:
                client.wait_success(c).stop()

    for i, c in enumerate(containers):
        containers[i] = client.wait_success(c)


    summaries = client.list_host_summary()

    assert len(summaries) >= 3

    for s in summaries:
        host = hosts[s.hostId]

        #assert 'host' in s
        #assert s.hostId == s.host().id

        assert s.name == host.name
        assert s.description == host.description
        assert s.clusterSize is None
        assert s.accountId == host.accountId

        ips = host.ipAddresses()
        if len(ips):
            assert s.ipAddress == ips[0].address
        else:
            assert s.ipAddress is None
        assert s.state == host.state

        expected_states = {}
        for i in host.instances():
            if i.removed is not None:
                continue
            try:
                expected_states[i.state] += 1
            except KeyError:
                expected_states[i.state] = 1

        assert expected_states == dict(s.instanceStates)

        assert s.links['host'] == host.links['self']
        assert s.links['instances'] == host.links['instances']

        assert dict(s.actions) == dict(host.actions)

        assert s.id == s.self().id
