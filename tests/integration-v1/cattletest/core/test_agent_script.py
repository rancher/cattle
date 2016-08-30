from common_fixtures import *  # NOQA
import requests


def test_x_forwarded_for(client):
    t = client.create_registration_token()
    t = client.wait_success(t)
    assert t.state == 'active'

    headers = {'X-Forwarded-For': '1.1.1.1'}
    response = requests.post(t.registrationUrl, headers=headers)
    assert response.status_code == 200
    assert 'export DETECTED_CATTLE_AGENT_IP="1.1.1.1"' in response.text

    headers = {'X-Forwarded-For': '1.1.1.1, 2.2.2.2'}
    response = requests.post(t.registrationUrl, headers=headers)
    assert response.status_code == 200
    assert 'export DETECTED_CATTLE_AGENT_IP="1.1.1.1"' in response.text
