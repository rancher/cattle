from common_fixtures import *  # NOQA
from cattle import ApiError


RESOURCE_DIR = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                            'resources/certs')


def test_create_cert_basic(client):
    cert = _read_cert("san_domain_com.crt")
    key = _read_cert("san_domain_com.key")
    cert1 = client. \
        create_certificate(name=random_str(),
                           cert=cert,
                           key=key)
    cert1 = client.wait_success(cert1)
    assert cert1.state == 'active'
    assert cert1.cert == cert
    assert cert1.certFingerprint is not None
    assert cert1.expiresAt is not None
    assert cert1.CN is not None
    assert cert1.issuer is not None
    assert cert1.issuedAt is not None
    assert cert1.algorithm is not None
    assert cert1.version is not None
    assert cert1.serialNumber is not None
    assert cert1.keySize == 2048
    assert cert1.subjectAlternativeNames is not None


def test_dup_names(super_client, client):
    cert_input = _read_cert("san_domain_com.crt")
    key = _read_cert("san_domain_com.key")
    name = random_str()
    cert1 = super_client. \
        create_certificate(name=name,
                           cert=cert_input,
                           key=key)
    super_client.wait_success(cert1)
    assert cert1.name == name

    cert2 = client. \
        create_certificate(name=name,
                           cert=cert_input,
                           key=key)
    cert2 = super_client.wait_success(cert2)
    assert cert2.name == name
    assert cert2.accountId != cert1.accountId

    with pytest.raises(ApiError) as e:
        super_client. \
            create_certificate(name=name,
                               cert=cert_input,
                               key=key)
    assert e.value.error.status == 422
    assert e.value.error.code == 'NotUnique'


def test_create_cert_invalid_cert(client):
    cert = _read_cert("cert_invalid.pem")
    key = _read_cert("key.pem")
    with pytest.raises(ApiError) as e:
        client. \
            create_certificate(name=random_str(),
                               cert=cert,
                               key=key)
    # catch io error
    assert e.value.error.status == 500


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


def _read_cert(name):
    with open(os.path.join(RESOURCE_DIR, name)) as f:
        return f.read()


def test_update_cert(client):
    cert1 = _read_cert("enduser-example.com.crt")
    key1 = _read_cert("enduser-example.com.key")
    c1 = client. \
        create_certificate(name=random_str(),
                           cert=cert1,
                           key=key1)
    c1 = client.wait_success(c1)

    cert2 = _read_cert("san_domain_com.crt")
    key2 = _read_cert("san_domain_com.key")
    c2 = client.update(c1, cert=cert2, key=key2)
    c2 = client.wait_success(c2, 120)
    assert c2.certFingerprint is not None
    assert c2.expiresAt is not None
    assert c2.CN is not None
    assert c2.issuer is not None
    assert c2.issuedAt is not None
    assert c2.algorithm is not None
    assert c2.version is not None
    assert c2.serialNumber is not None
    assert c2.keySize == 2048
    assert c2.subjectAlternativeNames is not None

    assert c2.cert == cert2
    assert c2.certFingerprint != c1.certFingerprint
    assert c2.expiresAt != c1.expiresAt
    assert c2.CN != c1.CN
    assert c2.issuer != c1.issuer
    assert c2.issuedAt != c1.issuedAt
    assert c2.serialNumber != c1.serialNumber
