package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    StorageService storageService;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    JsonMapper jsonMapper;


    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService", "externalService", "dnsService" };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);
        
        validateEnvironment(service);

        validateSelector(request);

        validateMetadata(request);

        validateName(service);

        validateLaunchConfigs(service, request);

        validateIpsHostName(request);

        validateImage(request, service);

        validateRequestedVip(request);

        request = updateLbServiceDefaults(type, request);

        return super.create(type, request, next);
    }

    public ApiRequest updateLbServiceDefaults(String type, ApiRequest request) {
        if (!type.equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return request;
        }
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        
        setLbServiceHealthcheck(data);

        request.setRequestObject(data);
        return request;
    }

    @SuppressWarnings("unchecked")
    protected void setLbServiceHealthcheck(Map<String, Object> data) {
        if (data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> launchConfig = (Map<String, Object>)data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
            InstanceHealthCheck healthCheck = new InstanceHealthCheck();
            healthCheck.setPort(42);
            healthCheck.setInterval(2000);
            healthCheck.setHealthyThreshold(2);
            healthCheck.setUnhealthyThreshold(3);
            healthCheck.setResponseTimeout(2000);
            launchConfig.put(InstanceConstants.FIELD_HEALTH_CHECK, healthCheck);
            data.put(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG, launchConfig);
        }
    }

    protected void validateSelector(ApiRequest request) {
        String selectorContainer = DataUtils.getFieldFromRequest(request,
                ServiceDiscoveryConstants.FIELD_SELECTOR_CONTAINER,
                String.class);
        if (selectorContainer != null) {
            SelectorUtils.getSelectorConstraints(selectorContainer);
        }

        String selectorLink = DataUtils.getFieldFromRequest(request,
                ServiceDiscoveryConstants.FIELD_SELECTOR_LINK,
                String.class);
        if (selectorLink != null) {
            SelectorUtils.getSelectorConstraints(selectorLink);
        }
    }

    protected void validateMetadata(ApiRequest request) {

        Object metadata = DataUtils.getFieldFromRequest(request, ServiceDiscoveryConstants.FIELD_METADATA,
                Object.class);
        if (metadata != null) {
            try {
                String value = jsonMapper.writeValueAsString(metadata);
                if (value.length() > 1048576) {
                    throw new ValidationErrorException(ValidationErrorCodes.MAX_LIMIT_EXCEEDED,
                            ServiceDiscoveryConstants.FIELD_METADATA + " limit is 1MB");
                }
            } catch (IOException e) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Failed to serialize field " + ServiceDiscoveryConstants.FIELD_METADATA);
            }
        }
    }

    protected void validateEnvironment(Service service) {
        Environment env = objectManager.loadResource(Environment.class, service.getEnvironmentId());
        List<String> invalidStates = Arrays.asList(CommonStatesConstants.ERROR, CommonStatesConstants.REMOVED,
                CommonStatesConstants.REMOVING);
        if (invalidStates.contains(env.getState())) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE, 
                    InstanceConstants.FIELD_ENVIRONMENT);
        }
    }
    
    protected void validateImage(ApiRequest request, Service service) {
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object imageUuid = launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
            if (imageUuid != null && !imageUuid.toString().equalsIgnoreCase(ServiceDiscoveryConstants.IMAGE_NONE)) {
                if (!storageService.isValidUUID(imageUuid.toString())) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE,
                            InstanceConstants.FIELD_IMAGE_UUID);
                }
            }
        }
    }

    protected void validateRequestedVip(ApiRequest request) {
        String requestedVip = DataUtils.getFieldFromRequest(request, ServiceDiscoveryConstants.FIELD_VIP,
                String.class);
        if (requestedVip == null) {
            return;
        }
        String vipCidr = ntwkDao.getVIPSubnetCidr();
        if (!NetUtils.isIpInSubnet(vipCidr, requestedVip)) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                            "Requested VIP " + ServiceDiscoveryConstants.FIELD_VIP
                                    + " is outside of configured vip cidr range");
        }
    }

    protected void validateIpsHostName(ApiRequest request) {
        
        List<?> externalIps = DataUtils.getFieldFromRequest(request, ServiceDiscoveryConstants.FIELD_EXTERNALIPS,
                List.class);
        
        String hostName = DataUtils.getFieldFromRequest(request, ServiceDiscoveryConstants.FIELD_HOSTNAME,
                String.class);

        boolean isExternalIps = externalIps != null && !externalIps.isEmpty();
        boolean isHostName = hostName != null && !hostName.isEmpty();

        if (isExternalIps && isHostName) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    ServiceDiscoveryConstants.FIELD_EXTERNALIPS + " and "
                            + ServiceDiscoveryConstants.FIELD_HOSTNAME + " are mutually exclusive");
        }
    }
    
    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        Service service = objectManager.loadResource(Service.class, id);

        validateName(service);
        validateLaunchConfigs(service, request);
        validateSelector(request);

        return super.update(type, id, request, next);
    }

    protected void validateLaunchConfigs(Service service, ApiRequest request) {
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        validateLaunchConfigNameUnique(service, launchConfigs);
        validateLaunchConfigsCircularRefs(service, launchConfigs);
    }


    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> populateLaunchConfigs(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        List<Map<String, Object>> allLaunchConfigs = new ArrayList<>();
        Object primaryLaunchConfig = data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
                
        if (primaryLaunchConfig != null) {
            // remove the name from launchConfig
            String primaryName = ((Map<String, String>) primaryLaunchConfig).get("name");
            if (primaryName != null) {
                ((Map<String, String>) primaryLaunchConfig).remove("name");
            }
            allLaunchConfigs.add((Map<String, Object>) primaryLaunchConfig);
        }

        Object secondaryLaunchConfigs = data
                .get(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
                
        if (secondaryLaunchConfigs != null) {
            allLaunchConfigs.addAll((List<Map<String, Object>>) secondaryLaunchConfigs);
        }

        return allLaunchConfigs;
    }


    protected void validateLaunchConfigsCircularRefs(Service service, List<Map<String, Object>> launchConfigs) {
        Map<String, List<String>> launchConfigRefs = populateLaunchConfigRefs(service, launchConfigs);
        for (String launchConfigName : launchConfigRefs.keySet()) {
            validateLaunchConfigCircularRef(launchConfigName, launchConfigRefs, new ArrayList<String>());
        }
    }

    protected void validateLaunchConfigCircularRef(String launchConfigName,
            Map<String, List<String>> launchConfigRefs,
            List<String> alreadySeenReferences) {
        List<String> myRefs = launchConfigRefs.get(launchConfigName);
        alreadySeenReferences.add(launchConfigName);
        for (String myRef : myRefs) {
            if (!launchConfigRefs.containsKey(myRef)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        "LaunchConfigName");
            }

            if (alreadySeenReferences.contains(myRef)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        "CircularReference");
            }

            if (!launchConfigRefs.get(myRef).isEmpty()) {
                validateLaunchConfigCircularRef(myRef, launchConfigRefs, alreadySeenReferences);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, List<String>> populateLaunchConfigRefs(Service service,
            List<Map<String, Object>> launchConfigs) {
        Map<String, List<String>> launchConfigRefs = new HashMap<>();
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object launchConfigName = launchConfig.get("name");
            if (launchConfigName == null) {
                launchConfigName = service.getName();
            }
            List<String> refs = new ArrayList<>();
            Object networkFromLaunchConfig = launchConfig
                    .get(ServiceDiscoveryConstants.FIELD_NETWORK_LAUNCH_CONFIG);
            if (networkFromLaunchConfig != null) {
                refs.add((String) networkFromLaunchConfig);
            }
            Object volumesFromLaunchConfigs = launchConfig
                    .get(ServiceDiscoveryConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);
            if (volumesFromLaunchConfigs != null) {
                refs.addAll((List<String>) volumesFromLaunchConfigs);
            }

            launchConfigRefs.put(launchConfigName.toString(), refs);
        }
        return launchConfigRefs;
    }


    protected void validateLaunchConfigNameUnique(Service service, List<Map<String, Object>> launchConfigs) {
        List<String> usedNames = new ArrayList<>();
        usedNames.add(service.getName());
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object secondaryName = launchConfig.get("name");
            if (secondaryName != null) {
                if (usedNames.contains(secondaryName)) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                            "name");
                }
                usedNames.add(secondaryName.toString());
            }
        }
    }


    protected void validateName(Service service) {
        List<? extends Service> existingSvcs = objectManager.find(Service.class, SERVICE.NAME, service.getName(),
                SERVICE.REMOVED, null, SERVICE.ENVIRONMENT_ID, service.getEnvironmentId());
        for (Service existingSvc : existingSvcs) {
            if (existingSvc.getId().equals(service.getId())) {
                continue;
            }
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                    "name");
        }

        if (service.getName() != null && (service.getName().startsWith("-") || service.getName().endsWith("-"))) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }
    }
}
