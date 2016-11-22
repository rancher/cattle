package io.cattle.platform.iaas.api.filter.service;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceDao.ServiceLink;
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

import com.google.common.base.Strings;

public class ServiceMappingsOutputFilter extends CachedOutputFilter<Map<Long, ServiceMappingsOutputFilter.ServiceInfo>> {

    @Inject
    ServiceDao serviceDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {Service.class};
    }

    @Override
    public String[] getTypes() {
        return new String[]{};
    }

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (original instanceof Service) {
            Service service = (Service)original;
            Map<Long, ServiceInfo> data = getCached(request);
            if (data == null) {
                return converted;
            }

            ServiceInfo info  = data.get(service.getId());
            if (info == null) {
                return converted;
            }

            IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
            Map<String, Object> fields = converted.getFields();
            fields.put(ServiceConstants.FIELD_INSTANCE_IDS, info.instanceIds);
            Map<String, Object> links = new HashMap<>();

            if (info.serviceLinks != null) {
                for (ServiceLink link : info.serviceLinks) {
                    String name = link.linkName;
                    if (Strings.isNullOrEmpty(name)) {
                        if (link.stackId.equals(service.getStackId())) {
                            name = link.serviceName;
                        } else {
                            name = String.format("%s/%s", link.stackName, link.serviceName);
                        }
                    }
                    links.put(name, idFormatter.formatId(ServiceConstants.KIND_SERVICE, link.serviceId));
                }
                fields.put(ServiceConstants.FIELD_LINKED_SERVICES, links);
            }
        }
        return converted;
    }

    @Override
    protected Map<Long, ServiceInfo> newObject(ApiRequest apiRequest) {
        IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
        List<Long> ids = getIds(apiRequest);
        Map<Long, List<ServiceLink>> mappings = serviceDao.getServiceLinks(ids);
        Map<Long, List<Object>> instances = serviceDao.getInstances(ids, idFormatter);

        Map<Long, ServiceInfo> result = new HashMap<>();
        for (Map.Entry<Long, List<ServiceLink>> entry : mappings.entrySet()) {
            result.put(entry.getKey(), new ServiceInfo(null, entry.getValue()));
            Map<String, Object> fields = new HashMap<>();
            fields.put(ServiceConstants.FIELD_LINKED_SERVICES, null);
        }

        for (Map.Entry<Long, List<Object>> entry : instances.entrySet()) {
            ServiceInfo info = result.get(entry.getKey());
            if (info == null) {
                info = new ServiceInfo(entry.getValue(), null);
                result.put(entry.getKey(), info);
            } else {
                info.instanceIds = entry.getValue();
            }
        }

        return result;
    }

    public static class ServiceInfo {
        List<Object> instanceIds;
        List<ServiceLink> serviceLinks;
        public ServiceInfo(List<Object> instanceIds, List<ServiceLink> serviceLinks) {
            super();
            this.instanceIds = instanceIds;
            this.serviceLinks = serviceLinks;
        }
    }

    @Override
    protected Long getId(Object obj) {
        if (obj instanceof Service) {
            return ((Service) obj).getId();
        }
        return null;
    }

}