package io.cattle.platform.servicediscovery.service.lookups;

import io.cattle.platform.core.model.Service;

import java.util.Arrays;
import java.util.Collection;

public class ServiceImplLookup implements ServiceLookup {

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (obj instanceof Service) {
            return Arrays.asList((Service)obj);
        }
        return null;
    }

}
