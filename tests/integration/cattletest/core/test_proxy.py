from common_fixtures import *  # NOQA

import requests


def test_proxy(client, admin_user_client):
    domain = 'releases.rancher.com'
    s = admin_user_client.by_id_setting('api.proxy.whitelist')

    if domain not in s.value:
        s.value += ',{}'.format(domain)
        admin_user_client.update(s, value=s.value)

    def func():
        s = admin_user_client.by_id_setting('api.proxy.whitelist')
        return domain in s.activeValue

    wait_for(func)

    base_url = client.schema.types['schema'].links['collection']
    base_url = base_url.replace('/schemas', '')

    r = requests.get(base_url + '/proxy/{}/{}'.format(domain,
                                                      'ui/latest/humans.txt'),
                     headers=auth_header_map(client))

    assert r.status_code == 200
    assert 'rancher' in r.text
