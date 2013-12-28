from dstack.type_manager import *


class Router:
    def __init__(self):
        pass

    def route(self, req):
        for handler in _handlers(req):
            resp = handler.execute(req)
            if resp is not None:
                return resp


def _handlers(req):
    for pre in get_type_list(PRE_REQUEST_HANDLER):
        yield pre

    drivers = []
    if req.name.startswith("storage."):
        drivers = get_type_list(STORAGE_DRIVER)

    if req.name.startswith("compute."):
        drivers = get_type_list(COMPUTE_DRIVER)

    for driver in drivers:
        if driver.supports(req):
            yield driver

    for post in get_type_list(POST_REQUEST_HANDLER):
        yield post
