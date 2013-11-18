class TypeFactory:
    @staticmethod
    def get_type(name, default=None):
        # Avoid circular dependencies on modules
        from marshaller import Marshaller
        from storage_manager import StorageManager

        if not default is None:
            return default

        try:
            t = locals()[name]
            return t()
        except KeyError, e:
            raise Exception("Failed to find type [%s]" % name)

    @staticmethod
    def get_compute_driver(host, req = None):
        from docker.compute import DockerCompute
        return DockerCompute()

    @staticmethod
    def get_storage_driver(pool, req = None):
        return None
