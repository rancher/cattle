package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.lock.definition.AbstractMultiLockDefinition;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ServicesSidekickLock extends AbstractMultiLockDefinition {

    public ServicesSidekickLock(List<Service> services) {
        super(getLockDefinitions(services));
    }

    protected static LockDefinition[] getLockDefinitions(List<Service> services) {
        LockDefinition[] result = new LockDefinition[services.size()];

        // sort so we don't run into situation when 2 threads try to acquire lock in diff order
        Collections.sort(services, new Comparator<Service>() {
            @Override
            public int compare(final Service s1, final Service s2) {
                return s1.getId().compareTo(s2.getId());
            }
        });

        int i = 0;
        for (Service service : services) {
            result[i++] = new ServiceLock(service);
        }

        return result;
    }
}
