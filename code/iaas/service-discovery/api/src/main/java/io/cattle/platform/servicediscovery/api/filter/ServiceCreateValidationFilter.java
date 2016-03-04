package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.ScalePolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    
    private static final int LB_HEALTH_CHECK_PORT = 42;

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

        validateLaunchConfigs(service, request);

        validateIpsHostName(request);

        validateImage(request, service);

        validatePorts(service, type, request);

        request = setHealthCheck(type, request);
        
        validateScalePolicy(service, request, false);

        return super.create(type, request, next);
    }
    
    public void validatePorts(Service service, String type, ApiRequest request) {
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        for (Map<String, Object> launchConfig : launchConfigs) {
            if (launchConfig.get(InstanceConstants.FIELD_PORTS) != null) {
                List<?> ports = (List<?>) launchConfig.get(InstanceConstants.FIELD_PORTS);
                for (Object port : ports) {
                    /* This will parse the PortSpec and throw an error */
                    PortSpec portSpec = new PortSpec(port.toString());
                    if (type.equals("loadBalancerService") && portSpec.getPublicPort() != null
                            && portSpec.getPublicPort().equals(LB_HEALTH_CHECK_PORT)) {
                        throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                                "Port " + LB_HEALTH_CHECK_PORT + " is reserved for loadBalancerService health check");
                    }
                }
            }
        }
    }
    
    protected void validateScalePolicy(Service service, ApiRequest request, boolean forUpdate) {
        Integer scale = DataUtils.getFieldFromRequest(request,
                ServiceDiscoveryConstants.FIELD_SCALE,
                Integer.class);
        if (scale == null && forUpdate) {
            scale = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_SCALE);
        }

        if (scale == null) {
            return;
        }

        Object policyObj = DataUtils.getFieldFromRequest(request,
                ServiceDiscoveryConstants.FIELD_SCALE_POLICY,
                Object.class);
        ScalePolicy policy = null;
        if (policyObj != null) {
            policy = jsonMapper.convertValue(policyObj,
                    ScalePolicy.class);
        } else if (forUpdate) {
            policy = DataAccessor.field(service,
                    ServiceDiscoveryConstants.FIELD_SCALE_POLICY, jsonMapper, ScalePolicy.class);
        }
        if (policy == null) {
            return;
        }

        if (policy.getMin().intValue() > policy.getMax().intValue()) {
            throw new ValidationErrorException(ValidationErrorCodes.MAX_LIMIT_EXCEEDED,
                    "Min scale can't exceed scale");
        }
    }


    @SuppressWarnings("unchecked")
    public ApiRequest setHealthCheck(String type, ApiRequest request) {
        if (!type.equalsIgnoreCase(ServiceDiscoveryConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return request;
        }
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        
        if (data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> launchConfig = (Map<String, Object>)data.get(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG);
            if (launchConfig.get(InstanceConstants.FIELD_HEALTH_CHECK) == null) {
                InstanceHealthCheck healthCheck = new InstanceHealthCheck();
                healthCheck.setPort(LB_HEALTH_CHECK_PORT);
                healthCheck.setInterval(2000);
                healthCheck.setHealthyThreshold(2);
                healthCheck.setUnhealthyThreshold(3);
                healthCheck.setResponseTimeout(2000);
                launchConfig.put(InstanceConstants.FIELD_HEALTH_CHECK, healthCheck);
                data.put(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG, launchConfig);
                request.setRequestObject(data);
            }
        }
        return request;
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
        List<String> invalidStates = Arrays.asList(InstanceConstants.STATE_ERROR, CommonStatesConstants.REMOVED,
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

        validateLaunchConfigs(service, request);
        validateSelector(request);
        validateScalePolicy(service, request, true);

        return super.update(type, id, request, next);
    }

    protected void validateLaunchConfigs(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object newName = data.get("name");
        String serviceName = newName != null ? newName.toString() : service.getName();
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        validateLaunchConfigNames(service, serviceName, launchConfigs);
        validateLaunchConfigsCircularRefs(service, serviceName, launchConfigs);
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


    protected void validateLaunchConfigsCircularRefs(Service service, String serviceName,
            List<Map<String, Object>> launchConfigs) {
        Map<String, Set<String>> launchConfigRefs = populateLaunchConfigRefs(service, serviceName, launchConfigs);
        for (String launchConfigName : launchConfigRefs.keySet()) {
            validateLaunchConfigCircularRef(launchConfigName, launchConfigRefs, new HashSet<String>());
        }
    }

    protected void validateLaunchConfigCircularRef(String launchConfigName,
            Map<String, Set<String>> launchConfigRefs,
            Set<String> alreadySeenReferences) {
        Set<String> myRefs = launchConfigRefs.get(launchConfigName);
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
    protected Map<String, Set<String>> populateLaunchConfigRefs(Service service, String serviceName,
            List<Map<String, Object>> launchConfigs) {
        Map<String, Set<String>> launchConfigRefs = new HashMap<>();
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object launchConfigName = launchConfig.get("name");
            if (launchConfigName == null) {
                launchConfigName = serviceName;
            }
            Set<String> refs = new HashSet<>();
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


    protected void validateLaunchConfigNames(Service service, String serviceName,
            List<Map<String, Object>> launchConfigs) {
        List<String> usedNames = new ArrayList<>();
        List<? extends Service> existingSvcs = objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID,
                service.getEnvironmentId(), SERVICE.REMOVED, null);
        for (Service existingSvc : existingSvcs) {
            if (existingSvc.getId().equals(service.getId())) {
                continue;
            }
            usedNames.add(existingSvc.getName().toLowerCase());
            for (String usedLcName : ServiceDiscoveryUtil.getServiceLaunchConfigNames(existingSvc)) {
                usedNames.add(usedLcName.toLowerCase());
            }
        }

        List<String> namesToValidate = new ArrayList<>();
        namesToValidate.add(serviceName.toLowerCase());
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object name = launchConfig.get("name");
            if (name != null) {
                namesToValidate.add(name.toString().toLowerCase());
            }
        }

        for (String name : namesToValidate) {
            validateName(name.toString());
            if (usedNames.contains(name)) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                        "name");
            }
            usedNames.add(name.toString().toLowerCase());
        }
    }

    protected void validateName(String name) {
       validateDNSPatternForName(name);
    }
}
