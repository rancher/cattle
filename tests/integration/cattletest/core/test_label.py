from common_fixtures import *  # NOQA


def _clean_hostlabelmaps_for_host(client, host):
    for label in host.labels():
        host.removelabel(label=label.id)
    wait_for_condition(
        client, host,
        lambda x: len(x.labels()) == 0,
        lambda x: 'Number of labels for host is: ' + len(x.labels()))


def test_add_remove_host_label(super_client, context):
    host = context.host
    _clean_hostlabelmaps_for_host(super_client, host)

    host.addlabel(key='location', value='basement')

    assert host.labels()[0].key == 'location'\
        and host.labels()[0].value == 'basement'

    # make sure duplicate entry is not made
    host.addlabel(key='location', value='basement')
    assert len(host.labels()) == 1

    host.addlabel(key='color', value='blue')
    assert len(host.labels()) == 2

    _clean_hostlabelmaps_for_host(super_client, host)
    assert len(host.labels()) == 0


def test_add_remove_container_label(super_client, new_context):
    host = new_context.host
    image_uuid = new_context.image_uuid

    c = super_client.create_container(imageUuid=image_uuid,
                                      requestedHostId=host.id)
    c.addlabel(key='func', value='web')
    assert c.instanceLabels()[0].key == 'func' \
        and c.instanceLabels()[0].value == 'web'

    # make sure duplicate entry is not made
    c.addlabel(key='func', value='web')
    assert len(c.instanceLabels()) == 1

    c.addlabel(key='nom', value='son')
    assert len(c.instanceLabels()) == 2

    c.removelabel(label=c.instanceLabels()[1].id)
    c.removelabel(label=c.instanceLabels()[0].id)

    wait_for_condition(
        super_client, c,
        lambda x: len(x.instanceLabels()) == 0,
        lambda x: 'Number of labels for container is: ' + len(x.labels()))


def test_set_container_labels(client, context):
    host = context.host
    image_uuid = context.image_uuid

    c = client.create_container(imageUuid=image_uuid,
                                requestedHostId=host.id)
    labels = {'role': 'web',
              'size': '4'}
    c.setlabels(labels=labels)

    wait_for_condition(
        client, c,
        lambda x: len(x.instanceLabels()) == 2,
        lambda x: 'Number of labels for container is: ' +
                  len(x.instanceLabels()))

    _assert_labels(c.instanceLabels(), labels)

    new_labels = {'role': 'web+db',
                  'nom': 'foobar'}
    c.setlabels(labels=new_labels)
    wait_for_condition(
        client, c,
        lambda x: len(x.instanceLabels()) == 2,
        lambda x: 'Number of labels for container is: ' +
                  len(x.instanceLabels()))

    _assert_labels(c.instanceLabels(), new_labels)


def test_set_host_labels(client, context):
    host = context.host
    _clean_hostlabelmaps_for_host(client, host)

    labels = {'location': 'closet',
              'cpus': '4'}
    host.setlabels(labels=labels)

    wait_for_condition(
        client, host,
        lambda x: len(x.labels()) == 2,
        lambda x: 'Number of labels for host is: ' + len(x.labels()))

    _assert_labels(host.labels(), labels)

    new_labels = {'location': 'attic',
                  'memory': '16gb'}
    host.setlabels(labels=new_labels)
    wait_for_condition(
        client, host,
        lambda x: len(x.labels()) == 2,
        lambda x: 'Number of labels for host is: ' + len(x.labels()))

    _assert_labels(host.labels(), new_labels)


def _assert_labels(labels_list, checking_for_labels):
    labels_map = {}
    for label in labels_list:
        labels_map[label.key] = label.value

    for k, v in checking_for_labels.items():
        assert labels_map.get(k) is not None and labels_map.get(k) == v
