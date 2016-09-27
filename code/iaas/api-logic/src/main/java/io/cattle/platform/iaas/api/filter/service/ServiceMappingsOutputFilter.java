package io.cattle.platform.iaas.api.filter.service;

import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.CachedOutputFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceMappingsOutputFilter extends CachedOutputFilter<Map<Long, Map<String, Object>>> {

    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

    @Override
    public String[] getTypes() {
        return new String[]{"service", "loadBalancerService", "dnsService", "externalService"};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Service) {
            Map<Long, Map<String, Object>> data = getCached(request);
            if (data != null) {
                Map<String, Object> fields = data.get(((Service) original).getId());
                if (fields != null) {
                    converted.getFields().putAll(fields);
                }
            }
        }
        return converted;
    }

    @Override
    protected Map<Long, Map<String, Object>> newObject(ApiRequest apiRequest) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
        List<Long> ids = getIds(apiRequest);
        Map<Long, ServiceDao.ServiceMapping> mappings = serviceDao.getServicesMappings(ids, idFormatter);
        Map<Long, List<Object>> instances = serviceDao.getInstances(ids, idFormatter);

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<Long, ServiceDao.ServiceMapping> entry : mappings.entrySet()) {
            Map<String, Object> fields = new HashMap<>();
            fields.put("consumedServiceIds", entry.getValue().consumed);
            fields.put("consumedByServiceIds", entry.getValue().consumedBy);
            result.put(entry.getKey(), fields);
        }
        for (Map.Entry<Long, List<Object>> entry : instances.entrySet()) {
            Map<String, Object> fields = result.get(entry.getKey());
            if (fields == null) {
                fields = new HashMap<>();
                result.put(entry.getKey(), fields);
            }
            fields.put("instanceIds", entry.getValue());
        }

        return result;
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Service) {
            return ((Service) obj).getId();
        }
        return null;
    }



}
