package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceUpgradeValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService", "dnsService" };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equals(ServiceDiscoveryConstants.ACTION_SERVICE_UPGRADE)) {
            Service service = objectManager.loadResource(Service.class, request.getId());
            ServiceUpgrade upgrade = jsonMapper.convertValue(request.getRequestObject(),
                    ServiceUpgrade.class);
            boolean toService = upgrade.getToServiceId() != null;
            boolean inService = upgrade.getLaunchConfig() != null || upgrade.getSecondaryLaunchConfigs() != null;
            if (toService && inService) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Service id and launch configs can't be specified together");
            }

            if (!toService && !inService) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Either toServiceId or launchConfig/secondaryLaunchConfigs params need to be specified");
            }

            // Today, we just check for launchConfig presence in the request, not on the diff between existing and new
            // launchConfig, to update the version to support the case when image:latest needs an update
            updatePrimaryLaunchConfig(upgrade, service);

            updateSecondaryLaunchConfigs(upgrade, service);

            objectManager.persist(service);
        }

        return super.resourceAction(type, request, next);
    }

    @SuppressWarnings("unchecked")
    protected void updateSecondaryLaunchConfigs(ServiceUpgrade upgrade, Service service) {
        Object newSecondaryLaunchConfigs = upgrade.getSecondaryLaunchConfigs();
        Map<String, Map<String, Object>> newSecondaryLaunchConfigsNames = new HashMap<>();
        if (newSecondaryLaunchConfigs != null) {
            for (Map<String, Object> newSecondaryLaunchConfig : (List<Map<String, Object>>) newSecondaryLaunchConfigs) {
                newSecondaryLaunchConfigsNames.put(newSecondaryLaunchConfig.get("name").toString(),
                        newSecondaryLaunchConfig);
            }
        }

        List<String> launchConfigNames = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
        for (String newSecondaryLaunchConfigName : newSecondaryLaunchConfigsNames.keySet()) {
            if (!launchConfigNames.contains(newSecondaryLaunchConfigName)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "Invalid secondary launch config name " + newSecondaryLaunchConfigName);
            }
        }

        if (!newSecondaryLaunchConfigsNames.isEmpty()) {
            List<Map<String, Object>> secondaryLaunchConfigsToUpgrade = (List<Map<String, Object>>) DataAccessor
                    .fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                    .withDefault(Collections.EMPTY_LIST)
                    .as(List.class);

            if (!secondaryLaunchConfigsToUpgrade.isEmpty()) {
                for (Map<String, Object> secondaryLaunchConfigToUpgrade : secondaryLaunchConfigsToUpgrade) {
                    Map<String, Object> newSecondaryLaunchConfig = newSecondaryLaunchConfigsNames
                            .get(secondaryLaunchConfigToUpgrade.get("name"));
                    if (newSecondaryLaunchConfig != null) {
                        upgradeLaunchConfigFields(service, secondaryLaunchConfigToUpgrade, newSecondaryLaunchConfig);
                    }
                }
                DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                        .set(secondaryLaunchConfigsToUpgrade);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void updatePrimaryLaunchConfig(ServiceUpgrade upgrade, Service service) {
        Object newLaunchConfigObj = upgrade.getLaunchConfig();
        if (newLaunchConfigObj != null) {
            Map<String, Object> newLaunchConfig = (Map<String, Object>) newLaunchConfigObj;
            Map<String, Object> launchConfigToUpgrade = (Map<String, Object>) DataAccessor.fields(service)
                    .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                    .as(Map.class);
            if (launchConfigToUpgrade != null) {
                upgradeLaunchConfigFields(service, launchConfigToUpgrade, newLaunchConfig);
                DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG)
                        .set(launchConfigToUpgrade);
            }
        }
    }

    protected void upgradeLaunchConfigFields(Service service, Map<String, Object> launchConfigToUpgrade,
            Map<String, Object> newLaunchConfig) {
        Integer version = new Integer(Integer.valueOf(launchConfigToUpgrade.get(
                ServiceDiscoveryConstants.FIELD_VERSION)
                .toString()));
        for (String key : newLaunchConfig.keySet()) {
            launchConfigToUpgrade.put(key, newLaunchConfig.get(key));
        }
        launchConfigToUpgrade.put(ServiceDiscoveryConstants.FIELD_VERSION, String.valueOf(version + 1));
    }

}
