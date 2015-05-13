from common_fixtures import *  # NOQA


def _clean_hostlabelmaps_for_host(client, host):
    for label in host.labels():
        host.removelabel(label=label.id)
    wait_for_condition(
        client, host,
        lambda x: len(x.labels()) == 0,
        lambda x: 'Number of labels for host is: ' + len(x.labels()))


def test_add_remove_host_label(super_client, sim_context):
    host = sim_context['host']
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


def test_add_remove_container_label(admin_client, sim_context):
    host = sim_context['host']
    image_uuid = sim_context['imageUuid']

    c = admin_client.create_container(imageUuid=image_uuid,
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
        admin_client, c,
        lambda x: len(x.instanceLabels()) == 0,
        lambda x: 'Number of labels for container is: ' + len(x.labels()))
