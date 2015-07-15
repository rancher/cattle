from common_fixtures import *  # NOQA
from cattle import ApiError


RESOURCE_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            'resources/certs')


def test_create_cert(client):
    cert = _read_cert("cert.pem")
    key = _read_cert("key.pem")
    cert1 = client. \
        create_certificate(name=random_str(),
                           cert=cert,
                           key=key)
    cert1 = client.wait_success(cert1)
    assert cert1.state == 'active'
    assert cert1.cert == cert
    assert cert1.certFingerprint is not None
    return cert1


def test_create_cert_invalid_key(client):
    cert = _read_cert("cert.pem")
    key = _read_cert("key_invalid.pem")
    with pytest.raises(ApiError) as e:
        client. \
            create_certificate(name=random_str(),
                               cert=cert,
                               key=key)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidFormat'
    assert e.value.error.fieldName == 'cert'


def test_create_cert_invalid_cert(client):
    cert = _read_cert("cert_invalid.pem")
    key = _read_cert("key.pem")
    with pytest.raises(ApiError) as e:
        client. \
            create_certificate(name=random_str(),
                               cert=cert,
                               key=key)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidFormat'
    assert e.value.error.fieldName == 'cert'


def test_create_cert_chain(client):
    cert = _read_cert("enduser-example.com.crt")
    key = _read_cert("enduser-example.com.key")
    chain = _read_cert("enduser-example.com.chain")
    cert1 = client. \
        create_certificate(name=random_str(),
                           cert=cert,
                           key=key,
                           certChain=chain)
    cert1 = client.wait_success(cert1)
    assert cert1.state == 'active'
    assert cert1.cert == cert
    return cert1


def test_invalid_key_cert_in_cert_chain(client):
    cert = _read_cert("cert.pem")
    key = _read_cert("key.pem")
    chain = _read_cert("enduser-example.com.chain")
    with pytest.raises(ApiError) as e:
        client. \
            create_certificate(name=random_str(),
                               cert=cert,
                               key=key,
                               certChain=chain)
    assert e.value.error.status == 422
    assert e.value.error.code == 'InvalidFormat'
    assert e.value.error.fieldName == 'certChain'


def _read_cert(name):
    with open(os.path.join(RESOURCE_DIR, name)) as f:
        return f.read()
