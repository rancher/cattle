
def has_service(instance, kind):
    try:
        for nic in instance.nics:
            if nic.deviceNumber != 0:
                continue

            for service in nic.network.networkServices:
                if service.kind == kind:
                    return True
    except (KeyError, AttributeError):
        pass

    return False
