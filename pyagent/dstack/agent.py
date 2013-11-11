#!/usr/bin/env python2.7

import sys
from type_factory import TypeFactory

class Agent(object):
    def __init__(self, marshaller = None, compute_manager = None, storage_manager = None):
        self._marshaller = TypeFactory.get_type("Marshaller", marshaller)
        #self._compute_manager = TypeFactory.get_type("ComputeManager", compute_manager)
        self._storage_manager = TypeFactory.get_type("StorageManager", storage_manager)

    def execute(self, line):
        req = self._marshaller.from_string(line)
        if req is None:
            raise Exception("Failed to unmarshall [%s]" % line)

        n = req.name
        print "Name:", n, req

        if req.name.startswith("storage."):
            pool = self._storage_manager.find_pool(req.data.storagePool)
            pool.handle(req)


        if req.name.startswith("compute."):
            compute_driver = TypeFactory.get_compute_driver(req.host, req)
            if compute_driver is None:
                raise 

        resp = object()
        return self._marshaller.to_string(resp)
