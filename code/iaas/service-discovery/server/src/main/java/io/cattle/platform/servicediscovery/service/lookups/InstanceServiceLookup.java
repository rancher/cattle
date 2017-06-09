package io.cattle.platform.servicediscovery.service.lookups;

import io.cattle.platform.core.model.Instance;

import java.util.Arrays;
import java.util.Collection;

public class InstanceServiceLookup implements ServiceLookup {

    @Override
    public Collection<Long> getServices(Object obj) {
        if (!(obj instanceof Instance)) {
            return null;
        }
        Instance instance = (Instance) obj;
        return Arrays.asList(instance.getServiceId());
    }

}
