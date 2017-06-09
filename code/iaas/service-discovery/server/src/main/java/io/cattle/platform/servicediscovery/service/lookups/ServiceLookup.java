package io.cattle.platform.servicediscovery.service.lookups;

import java.util.Collection;

public interface ServiceLookup {

    Collection<Long> getServices(Object obj);

}
