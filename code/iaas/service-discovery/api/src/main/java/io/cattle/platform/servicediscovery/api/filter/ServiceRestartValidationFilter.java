package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.RollingRestartStrategy;
import io.cattle.platform.core.addon.ServiceRestart;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceRestartValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService" };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_RESTART)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            ServiceRestart restart = jsonMapper.convertValue(request.getRequestObject(),
                    ServiceRestart.class);

            RollingRestartStrategy strategy = restart.getRollingRestartStrategy();
            if (strategy == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED,
                        "Restart strategy needs to be set");
            }

            Map<Long, Long> instanceToStartCount = new HashMap<>();
            for (Instance instance : exposeMapDao.listServiceManagedInstances(service)) {
                instanceToStartCount.put(instance.getId(), instance.getStartCount());
            }

            strategy.setInstanceToStartCount(instanceToStartCount);
            restart.setRollingRestartStrategy(strategy);
            request.setRequestObject(jsonMapper.writeValueAsMap(restart));
            objectManager.persist(service);
        }

        return super.resourceAction(type, request, next);
    }
}
