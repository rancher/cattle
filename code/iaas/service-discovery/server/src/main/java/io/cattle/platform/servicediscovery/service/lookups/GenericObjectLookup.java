package io.cattle.platform.servicediscovery.service.lookups;

import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;

public class GenericObjectLookup extends AbstractJooqDao implements ServiceLookup {

    @Inject
    ObjectManager objectManager;

    @Override
    public Collection<? extends Service> getServices(Object obj) {
        if (!(obj instanceof GenericObject)) {
            return null;
        }
        Long id = DataAccessor.fieldLong(obj, "serviceId");
        if (id != null) {
            Service service = objectManager.loadResource(Service.class, id);
            if (service != null) {
                return Arrays.asList(service);
            }
        }

        return null;
    }

}
