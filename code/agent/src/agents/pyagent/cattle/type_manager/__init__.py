TYPES = {}

PRIORITY_PRE = 500
PRIORITY_SPECIFIC = 1000
PRIORITY_DEFAULT_OVERRIDE = 1500
PRIORITY_DEFAULT = 2000

PUBLISHER = 'publisher'
MARSHALLER = 'marshaller'
ROUTER = 'router'
STORAGE_DRIVER = 'storage_driver'
COMPUTE_DRIVER = 'compute_driver'
PRE_REQUEST_HANDLER = 'pre_request_handler'
POST_REQUEST_HANDLER = 'post_request_handler'
LIFECYCLE = 'lifecycle'


def types():
    for v in TYPES.values():
        for i in v:
            yield i


def register_type(type_name, impl):
    priority = _get_priority(impl)
    try:
        types = TYPES[type_name]
        for i in range(len(types)):
            if priority < _get_priority(types[i]):
                types.insert(i, types)
                break
        else:
            types.append(impl)
    except KeyError:
        TYPES[type_name] = [impl]


def _get_priority(impl):
    try:
        return impl.priority
    except AttributeError:
        return PRIORITY_SPECIFIC


def get_type(type_name):
    try:
        types = TYPES[type_name]
        if len(types) > 0:
            return types[0]
        else:
            return None
    except KeyError:
        return None


def get_type_list(type_name):
    try:
        return TYPES[type_name]
    except KeyError:
        TYPES[type_name] = []
        return get_type_list(type_name)
