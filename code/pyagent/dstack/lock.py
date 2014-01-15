import portalocker
import os

LOCK_DIR = "locks"

if not os.path.exists(LOCK_DIR):
    os.mkdir(LOCK_DIR)


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
        try:
            if os.path.exists(self._name):
                os.unlink(self._name)
        except:
            pass

        return self._lock.__exit__(type, value, tb)


def lock(obj):
    if isinstance(obj, basestring):
        lock_name = obj
    else:
        lock_name = "{0}-{1}".format(obj["type"], obj["id"])

    return LockWrapper(lock_name, portalocker.Lock(os.path.join(LOCK_DIR, lock_name)))