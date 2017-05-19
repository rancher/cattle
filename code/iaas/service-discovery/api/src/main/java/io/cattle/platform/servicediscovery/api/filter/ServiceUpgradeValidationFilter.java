package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.iaas.api.service.RevisionDiffomatic;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.storage.service.StorageService;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceUpgradeValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    RevisionManager revisionManager;
    @Inject
    StorageService storageService;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        List<String> supportedTypes = new ArrayList<>();
        supportedTypes.addAll(ServiceConstants.SERVICE_LIKE);
        supportedTypes.add(ServiceConstants.KIND_DNS_SERVICE);
        supportedTypes.add(ServiceConstants.KIND_EXTERNAL_SERVICE);
        return supportedTypes.toArray(new String[supportedTypes.size()]);
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceConstants.ACTION_SERVICE_UPGRADE)) {
            return processInServiceUpgradeStrategy(type, request, next);
        }

        return super.resourceAction(type, request, next);
    }

    protected Object processInServiceUpgradeStrategy(String type, ApiRequest request, ResourceManager next) {
        Service service = objectManager.loadResource(Service.class, request.getId());
        ServiceUpgrade upgrade = jsonMapper.convertValue(request.getRequestObject(), ServiceUpgrade.class);
        InServiceUpgradeStrategy strategy = finalizeUpgradeStrategy(service, upgrade.getInServiceStrategy());
        Map<String, Object> data = jsonMapper.writeValueAsMap(strategy);

        revisionManager.setFieldsForUpgrade(data);

        RevisionDiffomatic diff = revisionManager.createNewRevision(request.getSchemaFactory(), service, data);
        objectManager.setFields(service, diff.getNewRevisionData());
        request.setRequestObject(diff.getNewRevisionData());
        service = revisionManager.assignRevision(diff, service);

        return super.resourceAction(type, request, next);
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
