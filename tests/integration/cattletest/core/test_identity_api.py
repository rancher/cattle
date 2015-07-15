from common_fixtures import *  # NOQA


def test_get_correct_identity(admin_user_client):
    name = "Identity User"
    context = create_context(admin_user_client, name=name)
    identities = context.user_client.list_identity()
    assert len(identities) == 1
    assert identities[0].name == name


def test_search_identity_name(admin_user_client):
    name_base = random_str()
    names = []
    name_format = "{} {}"
    for x in range(0, 5):
        rand_name = random_str()
        name = name_format.format(name_base, rand_name)
        names.append(name)
        create_context(admin_user_client, name=name)
    user_client = create_context(admin_user_client).user_client
    for name in names:
        ids = user_client\
            .list_identity(name=name)
        assert len(ids) == 1
        assert ids[0].name == name
        identity = user_client.by_id('identity', id=ids[0].id)
        assert identity.name == ids[0].name
        assert identity.id == ids[0].id
        assert identity.externalId == ids[0].externalId
        assert identity.externalIdType == ids[0].externalIdType


def test_search_identity_name_like(admin_user_client):
    name_base = random_str()
    names = []
    name_format = "{} {}"
    for x in range(0, 5):
        rand_name = random_str()
        name = name_format.format(name_base, rand_name)
        names.append(name)
        create_context(admin_user_client, name=name)
    identities = admin_user_client.list_identity(all=name_base)
    assert len(identities) == 5
    assert len(names) == 5
    found = 0
    for identity in identities:
        for name in names:
            if (identity.name == name):
                found += 1
    assert found == 5
