import portalocker
import os
from cattle import Config


class FailedToLock(Exception):
    pass


class LockWrapper(object):
    def __init__(self, name, lock):
        self._name = name
        self._lock = lock

    def __enter__(self):
        try:
            return self._lock.__enter__()
        except portalocker.AlreadyLocked:
            raise FailedToLock("Failed to lock [{0}]".format(self._name))

    def __exit__(self, type, value, tb):
        if os.path.exists(self._lock.filename):
            os.unlink(self._lock.filename)

        return self._lock.__exit__(type, value, tb)


def lock(obj):
    if isinstance(obj, basestring):
        lock_name = obj
    else:
        lock_name = "{0}-{1}".format(obj["type"], obj["id"])

    lock_dir = Config.lock_dir()
    if not os.path.exists(lock_dir):
        os.mkdir(lock_dir)
    return LockWrapper(lock_name,
                       portalocker.Lock(os.path.join(lock_dir, lock_name)))
