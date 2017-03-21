package io.cattle.platform.servicediscovery.service;

import io.cattle.platform.core.model.Service;

import java.util.Collection;

public interface ServiceLookup {
    Collection<? extends Service> getServices(Object obj);
}
