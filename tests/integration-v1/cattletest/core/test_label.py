from common import *  # NOQA


def _clean_hostlabelmaps_for_host(client, host):
    for label in host.hostLabels():
        host.removelabel(label=label.id)
    wait_for_condition(
        client, host,
        lambda x: len(x.hostLabels()) == 0,
        lambda x: 'Number of labels for host is: ' + len(x.hostLabels()))


def test_edit_host_label(super_client, context):
    host = context.host
    _clean_hostlabelmaps_for_host(super_client, host)
    new_labels = {'role': 'web+db',
                  'nom': 'foobar'}
    host = super_client.update(host, labels=new_labels)
    wait_for_condition(
        super_client, host,
        lambda x: len(x.hostLabels()) == 2,
        lambda x: 'Number of labels for host is: ' +
                  len(x.hostLabels()))

    _assert_labels(host.hostLabels(), new_labels)

    new_labels = {'role': 'web+db',
                  'foo': 'bar',
                  'loc': 'sf'}
    host = super_client.update(host, labels=new_labels)

    wait_for_condition(
        super_client, host,
        lambda x: len(x.hostLabels()) == 3 and
        _get_labels_map(x.hostLabels()).get('loc') == 'sf',
        lambda x: 'Host labels are: ' + str(_get_labels_map(x.hostLabels())))

    _assert_labels(host.hostLabels(), new_labels)


def _assert_labels(labels_list, checking_for_labels):
    labels_map = _get_labels_map(labels_list)

    for k, v in checking_for_labels.items():
        assert labels_map.get(k) is not None and labels_map.get(k) == v


def _get_labels_map(labels_list):
    labels_map = {}
    for label in labels_list:
        labels_map[label.key] = label.value

    return labels_map
