package io.cattle.platform.servicediscovery.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceResourceManager extends AbstractJooqResourceManager {
    
    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        Object object = super.updateInternal(type, id, obj, request);
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Long scale = (Long) data.get(ServiceDiscoveryConstants.FIELD_SCALE);
        if (scale != null) {
            List<Service> sidekickServices = exposeMapDao
                    .collectSidekickServices(objectManager.loadResource(Service.class, id), null);
            Iterator<Service> it = sidekickServices.iterator();
            while (it.hasNext()) {
                Service service = it.next();
                if (service.getId().toString().equals(id)) {
                    it.remove();
                }
            }
            exposeMapDao.updateScale(sidekickServices, scale.intValue());
        }
        return object;
    }
}
