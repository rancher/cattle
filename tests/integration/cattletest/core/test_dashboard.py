from common_fixtures import *  # NOQA


def test_dashboard(context):
    dash = context.client.list_dashboard()
    dash = dash.data[0]['host']
    assert dash['coreCount'] == 4
    assert dash['memoryTotalMB'] == 2002.262
    checkBucket(dash['cores'], 4)
    checkBucket(dash['memory'], 1)
    checkBucket(dash['mounts'], 1)
    checkBucket(dash['networkIn'], 1)
    checkBucket(dash['networkOut'], 1)
    pass


def checkBucket(listOfBuckets, count):
    counted = 0
    for bucket in listOfBuckets:
        counted += len(bucket['ids'])
    assert counted == count
