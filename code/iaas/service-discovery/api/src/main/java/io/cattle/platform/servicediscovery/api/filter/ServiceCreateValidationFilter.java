package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.ScalePolicy;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.storage.api.filter.ExternalTemplateInstanceFilter;
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

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ObjectManager objectManager;

    @Inject
    StorageService storageService;

    @Inject
    JsonMapper jsonMapper;

    private static final int LB_HEALTH_CHECK_PORT = 42;
    public static final DynamicStringProperty DEFAULT_REGISTRY = ArchaiusUtil.getString("registry.default");
    public static final DynamicStringProperty WHITELIST_REGISTRIES = ArchaiusUtil.getString("registry.whitelist");

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { ServiceConstants.KIND_SERVICE,
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE,
                ServiceConstants.KIND_EXTERNAL_SERVICE, ServiceConstants.KIND_DNS_SERVICE,
                ServiceConstants.KIND_NETWORK_DRIVER_SERVICE, ServiceConstants.KIND_STORAGE_DRIVER_SERVICE};
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);

        validateStack(service);

        validateSelector(request);

        validateMetadata(request);

        validateLaunchConfigs(service, request);

        validateIpsHostName(request);

        request = validateAndSetImage(request, service);

        validatePorts(service, type, request);

        request = setHealthCheck(type, request);
        
        validateScalePolicy(service, request, false);

        request = setServiceIndexStrategy(type, request);

        request = setLBServiceEnvVars(type, service, request);
        
        validateLbConfig(request, type);

        return super.create(type, request, next);
    }
    
    @SuppressWarnings("unchecked")
    public void validateLbConfig(ApiRequest request, String type) {
        // add lb information to the metadata
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }
        Map<String, Object> lbConfig = DataUtils.getFieldFromRequest(request, ServiceConstants.FIELD_LB_CONFIG,
                Map.class);
        if (lbConfig != null && lbConfig.containsKey(ServiceConstants.FIELD_PORT_RULES)) {
            List<PortRule> portRules = jsonMapper.convertCollectionValue(
                    lbConfig.get(ServiceConstants.FIELD_PORT_RULES), List.class, PortRule.class);
            for (PortRule rule : portRules) {
                // either serviceId or selector are required
                boolean emptySelector = StringUtils.isEmpty(rule.getSelector());
                boolean emptyService = StringUtils.isEmpty(rule.getServiceId());
                if (emptySelector && emptyService) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "serviceId");
                }
                if (!emptySelector && !emptyService) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                            "Can't specify both selector and serviceId");
                }

                if (!emptyService && rule.getTargetPort() == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "targetPort");
                }
            }
        }
    }

    public ApiRequest setServiceIndexStrategy(String type, ApiRequest request) {
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_SERVICE)) {
            return request;
        }
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        data.put(ServiceConstants.FIELD_SERVICE_INDEX_STRATEGY,
                ServiceConstants.SERVICE_INDEX_DU_STRATEGY);

        request.setRequestObject(data);
        return request;
    }

    @SuppressWarnings("unchecked")
    public ApiRequest setLBServiceEnvVars(String type, Service lbService, ApiRequest request) {
        if (!ServiceConstants.KIND_LOAD_BALANCER_SERVICE.equalsIgnoreCase(type)) {
            return request;
        }

        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) == null) {
            return request;
        }

        Map<String, Object> launchConfig = (Map<String, Object>) data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);

        Map<String, String> labels = new HashMap<>();
        Object labelsObj = launchConfig.get(InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            labels = (Map<String, String>) labelsObj;
        }

        labels.put(SystemLabels.LABEL_AGENT_ROLE, AgentConstants.ENVIRONMENT_ADMIN_ROLE);
        labels.put(SystemLabels.LABEL_AGENT_CREATE, "true");
        launchConfig.put(InstanceConstants.FIELD_LABELS, labels);
        data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, launchConfig);
        request.setRequestObject(data);
        return request;
    }

    @SuppressWarnings("unchecked")
    public void validatePorts(Service service, String type, ApiRequest request) {
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }
        Map<String, Object> lbConfig = DataUtils.getFieldFromRequest(request, ServiceConstants.FIELD_LB_CONFIG,
                Map.class);
        if (lbConfig == null) {
            return;
        }

        if (lbConfig != null && lbConfig.containsKey(ServiceConstants.FIELD_PORT_RULES)) {
            List<PortRule> portRules = jsonMapper.convertCollectionValue(
                    lbConfig.get(ServiceConstants.FIELD_PORT_RULES), List.class, PortRule.class);
            for (PortRule portRule : portRules) {
                // fixme check ports from port map
                if (portRule.getSourcePort() != null && portRule.getSourcePort().equals(LB_HEALTH_CHECK_PORT)) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                            "Port " + LB_HEALTH_CHECK_PORT + " is reserved for service health check");
                }
            }
        }
    }
    
    protected void validateScalePolicy(Service service, ApiRequest request, boolean forUpdate) {
        Integer scale = DataUtils.getFieldFromRequest(request,
                ServiceConstants.FIELD_SCALE,
                Integer.class);
        if (scale == null && forUpdate) {
            scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        }

        if (scale == null) {
            return;
        }

        Object policyObj = DataUtils.getFieldFromRequest(request,
                ServiceConstants.FIELD_SCALE_POLICY,
                Object.class);
        ScalePolicy policy = null;
        if (policyObj != null) {
            policy = jsonMapper.convertValue(policyObj,
                    ScalePolicy.class);
        } else if (forUpdate) {
            policy = DataAccessor.field(service,
                    ServiceConstants.FIELD_SCALE_POLICY, jsonMapper, ScalePolicy.class);
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
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return request;
        }
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Integer healthCheckPort = LB_HEALTH_CHECK_PORT;
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> launchConfig = (Map<String, Object>)data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
            if (launchConfig.get(InstanceConstants.FIELD_HEALTH_CHECK) == null) {
                InstanceHealthCheck healthCheck = new InstanceHealthCheck();
                healthCheck.setPort(healthCheckPort);
                healthCheck.setInterval(2000);
                healthCheck.setHealthyThreshold(2);
                healthCheck.setUnhealthyThreshold(3);
                healthCheck.setResponseTimeout(2000);
                launchConfig.put(InstanceConstants.FIELD_HEALTH_CHECK, healthCheck);
                data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, launchConfig);
                request.setRequestObject(data);
            }
        }
        return request;
    }

    protected void validateSelector(ApiRequest request) {
        String selectorContainer = DataUtils.getFieldFromRequest(request,
                ServiceConstants.FIELD_SELECTOR_CONTAINER,
                String.class);
        if (selectorContainer != null) {
            SelectorUtils.getSelectorConstraints(selectorContainer);
        }

        String selectorLink = DataUtils.getFieldFromRequest(request,
                ServiceConstants.FIELD_SELECTOR_LINK,
                String.class);
        if (selectorLink != null) {
            SelectorUtils.getSelectorConstraints(selectorLink);
        }
    }

    protected void validateMetadata(ApiRequest request) {

        Object metadata = DataUtils.getFieldFromRequest(request, ServiceConstants.FIELD_METADATA,
                Object.class);
        if (metadata != null) {
            try {
                String value = jsonMapper.writeValueAsString(metadata);
                if (value.length() > 1048576) {
                    throw new ValidationErrorException(ValidationErrorCodes.MAX_LIMIT_EXCEEDED,
                            ServiceConstants.FIELD_METADATA + " limit is 1MB");
                }
            } catch (IOException e) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Failed to serialize field " + ServiceConstants.FIELD_METADATA);
            }
        }
    }

    protected void validateStack(Service service) {
        Stack env = objectManager.loadResource(Stack.class, service.getStackId());
        List<String> invalidStates = Arrays.asList(InstanceConstants.STATE_ERROR, CommonStatesConstants.REMOVED,
                CommonStatesConstants.REMOVING);
        if (env == null || invalidStates.contains(env.getState())) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE, ServiceConstants.FIELD_STACK_ID);
        }
    }

    @SuppressWarnings("unchecked")
    protected ApiRequest validateAndSetImage(ApiRequest request, Service service) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> launchConfig = (Map<String, Object>)data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
            if (launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID) != null) {
                Object imageUuid = launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
                if (imageUuid != null && !imageUuid.toString().equalsIgnoreCase(ServiceConstants.IMAGE_NONE)) {
                    String fullImageName = ExternalTemplateInstanceFilter.getImageUuid(imageUuid.toString(), storageService);
                    launchConfig.put(InstanceConstants.FIELD_IMAGE_UUID, fullImageName);
                    data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, launchConfig);
                }
            }
        }

        List<Object> modifiedSlcs = new ArrayList<>();
        if (data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
           List<Object> slcs = (List<Object>)data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
           for (Object slcObj : slcs) {
                Map<String, Object> slc = (Map<String, Object>) slcObj;
                if (slc.get(InstanceConstants.FIELD_IMAGE_UUID) != null) {
                    Object imageUuid = slc.get(InstanceConstants.FIELD_IMAGE_UUID);
                    if (imageUuid != null && !imageUuid.toString().equalsIgnoreCase(ServiceConstants.IMAGE_NONE)) {
                        String fullImageName = ExternalTemplateInstanceFilter.getImageUuid(imageUuid.toString(), storageService);
                        slc.put(InstanceConstants.FIELD_IMAGE_UUID, fullImageName);
                    }
                }
                modifiedSlcs.add(slc);
            }

            data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, modifiedSlcs);
        }

        request.setRequestObject(data);
        return request;
    }

    protected void validateIpsHostName(ApiRequest request) {
        
        List<?> externalIps = DataUtils.getFieldFromRequest(request, ServiceConstants.FIELD_EXTERNALIPS,
                List.class);
        
        String hostName = DataUtils.getFieldFromRequest(request, ServiceConstants.FIELD_HOSTNAME,
                String.class);

        boolean isExternalIps = externalIps != null && !externalIps.isEmpty();
        boolean isHostName = hostName != null && !hostName.isEmpty();

        if (isExternalIps && isHostName) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    ServiceConstants.FIELD_EXTERNALIPS + " and "
                            + ServiceConstants.FIELD_HOSTNAME + " are mutually exclusive");
        }
    }
    
    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        Service service = objectManager.loadResource(Service.class, id);

        validateLaunchConfigs(service, request);
        validateSelector(request);
        validateLbConfig(request, type);
        validateScalePolicy(service, request, true);
        validatePorts(service, type, request);

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
        Object primaryLaunchConfig = data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
                
        if (primaryLaunchConfig != null) {
            // remove the name from launchConfig
            String primaryName = ((Map<String, String>) primaryLaunchConfig).get("name");
            if (primaryName != null) {
                ((Map<String, String>) primaryLaunchConfig).remove("name");
            }
            allLaunchConfigs.add((Map<String, Object>) primaryLaunchConfig);
        }

        Object secondaryLaunchConfigs = data
                .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
                
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
                    .get(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG);
            if (networkFromLaunchConfig != null) {
                refs.add((String) networkFromLaunchConfig);
            }
            Object volumesFromLaunchConfigs = launchConfig
                    .get(ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG);
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
        List<? extends Service> existingSvcs = objectManager.find(Service.class, SERVICE.STACK_ID,
                service.getStackId(), SERVICE.REMOVED, null);
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
