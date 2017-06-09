package io.cattle.platform.loop.trigger;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.loop.LoopFactoryImpl;
import io.cattle.platform.servicediscovery.service.lookups.ServiceLookup;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceReconcileTrigger implements Trigger {

    @Inject
    LoopManager loopManager;
    @Inject
    List<ServiceLookup> serviceLookups;

    @Override
    public void trigger(ProcessInstance process) {
        Object resource = process.getResource();
        if (resource == null) {
            return;
        }

        Set<Long> services = new HashSet<>();
        for (ServiceLookup lookup : serviceLookups) {
            Collection<Long> lookupSvs = lookup.getServices(resource);
            if (lookupSvs != null) {
                services.addAll(lookupSvs);
            }
        }

        for (Long id : services) {
            loopManager.kick(LoopFactoryImpl.RECONCILE, ServiceConstants.KIND_SERVICE, id, resource);
        }
    }

}