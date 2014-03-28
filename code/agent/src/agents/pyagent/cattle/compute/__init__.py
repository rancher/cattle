from cattle.agent.handler import BaseHandler
from cattle.progress import Progress


class BaseComputeDriver(BaseHandler):
    def _get_handler_category(self, req):
        return "compute"

    def instance_activate(self, req=None, instanceHostMap=None, **kw):
        instance = instanceHostMap.instance
        host = instanceHostMap.host
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_instance_active(instance, host),
            result=lambda: self._get_response_data(instanceHostMap),
            lock_obj=instance,
            action=lambda: self._do_instance_activate(instance, host, progress)
        )

    def instance_deactivate(self, req=None, instanceHostMap=None, **kw):
        instance = instanceHostMap.instance
        host = instanceHostMap.host
        progress = Progress(req)

        return self._do(
            req=req,
            check=lambda: self._is_instance_inactive(instance, host),
            result=lambda: self._get_response_data(instanceHostMap),
            lock_obj=instance,
            action=lambda: self._do_instance_deactivate(instance, host,
                                                        progress)
        )

    def _is_instance_active(self, instance, host):
        raise Exception("Not implemented")

    def _do_instance_activate(self, instance, host):
        raise Exception("Not implemented")

    def _is_instance_inactive(self, instance, host):
        raise Exception("Not implemented")

    def _do_instance_deactivate(self, instance, host):
        raise Exception("Not implemented")
