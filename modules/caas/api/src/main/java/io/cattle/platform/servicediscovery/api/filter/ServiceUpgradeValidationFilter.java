package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.iaas.api.service.RevisionDiffomatic;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

public class ServiceUpgradeValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;
    JsonMapper jsonMapper;
    RevisionManager revisionManager;

    public ServiceUpgradeValidationFilter(ObjectManager objectManager, JsonMapper jsonMapper, RevisionManager revisionManager) {
        super();
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
        this.revisionManager = revisionManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request, ActionHandler next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_UPGRADE)) {
            return processInServiceUpgradeStrategy(name, obj, request, next);
        }

        return super.perform(name, obj, request, next);
    }

    protected Object processInServiceUpgradeStrategy(String name, Object obj, ApiRequest request, ActionHandler next) {
        Service service = objectManager.loadResource(Service.class, request.getId());
        ServiceUpgrade upgrade = jsonMapper.convertValue(request.getRequestObject(), ServiceUpgrade.class);
        InServiceUpgradeStrategy strategy = finalizeUpgradeStrategy(service, upgrade.getInServiceStrategy());
        Map<String, Object> data = jsonMapper.writeValueAsMap(strategy);

        revisionManager.setFieldsForUpgrade(data);

        RevisionDiffomatic diff = revisionManager.createNewRevision(request.getSchemaFactory(), service, data);
        objectManager.setFields(service, diff.getNewRevisionData());
        request.setRequestObject(diff.getNewRevisionData());
        service = revisionManager.assignRevision(diff, service);

        return super.perform(name, obj, request, next);
    }

    @SuppressWarnings("unchecked")
    protected InServiceUpgradeStrategy finalizeUpgradeStrategy(Service service, InServiceUpgradeStrategy strategy) {
        if (strategy.getLaunchConfig() == null && strategy.getSecondaryLaunchConfigs() == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    "LaunchConfig/secondaryLaunchConfigs need to be specified for inService strategy");
        }

        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_RETAIN_IP)) {
            if (strategy.getStartFirst()) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "StartFirst option can't be used for service with "
                                + ServiceConstants.FIELD_RETAIN_IP + " field set");
            }
        }

        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            if (strategy.getLaunchConfig() == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "LaunchConfig is required for load balancer service");
            }
            ServiceUtil.injectBalancerLabelsAndHealthcheck((Map<Object, Object>) strategy.getLaunchConfig());
        }

        return strategy;
    }

}
