package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ResourceManagerLocator locator;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);
        
        // 1. name should be unique within the environment
        ResourceManager rm = locator.getResourceManagerByType(type);
        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(ObjectMetaDataManager.NAME_FIELD, service.getName());
        criteria.put(ObjectMetaDataManager.REMOVED_FIELD, new Condition(ConditionType.NULL));
        criteria.put(ServiceDiscoveryConstants.FIELD_ENVIRIONMENT_ID, service.getEnvironmentId());

        List<?> existingSvcs = rm.list(type, criteria, null);
        if (!existingSvcs.isEmpty()) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "name");
        }

        if (service.getName().startsWith("-") || service.getName().endsWith("-")) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }

        // 2. if a part of sidekick, scale should match other sidekick participants scale
        Map<String, Object> launchConfig = DataAccessor
                .fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).as(Map.class);
        Map<String, String> labels = launchConfig.get(InstanceConstants.FIELD_LABELS) == null ? new HashMap<String, String>()
                : (Map<String, String>) launchConfig.get(InstanceConstants.FIELD_LABELS);
        List<Service> sidekickServices = exposeMapDao.collectSidekickServices(service, labels);
        Integer requestedScale = DataAccessor.fromMap(request.getRequestObject())
                .withKey(ServiceDiscoveryConstants.FIELD_SCALE)
                .as(Integer.class);
        Integer existingMaxScale = null;

        // find max scale among existing services
        for (Service sidekickService : sidekickServices) {
            // skip itself
            if (sidekickService.getId() == null) {
                continue;
            }
            Integer scale = DataAccessor.fields(sidekickService).withKey(ServiceDiscoveryConstants.FIELD_SCALE)
                    .as(Integer.class);
            if (existingMaxScale == null) {
                existingMaxScale = scale;
                continue;
            }

            if (scale.intValue() > existingMaxScale.intValue()) {
                existingMaxScale = scale;
            }
        }

        // if requested scale doesn't match existing, default it to the max existing scale
        if (existingMaxScale != null && !requestedScale.equals(existingMaxScale)) {
            DataAccessor.fromMap(request.getRequestObject())
                    .withKey(ServiceDiscoveryConstants.FIELD_SCALE).set(existingMaxScale);
        }

        return super.create(type, request, next);
    }
}
