package io.cattle.platform.api.service;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.revision.RevisionDiffomatic;
import io.cattle.platform.revision.RevisionManager;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;

import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
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

import org.apache.commons.lang3.StringUtils;

public class ServiceCreateValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    StorageService storageService;
    JsonMapper jsonMapper;
    RevisionManager revisionManager;

    private static final int LB_HEALTH_CHECK_PORT = 42;
    private static final Set<String> DRIVER_TYPES = CollectionUtils.set(ServiceConstants.KIND_STORAGE_DRIVER_SERVICE,
            ServiceConstants.KIND_NETWORK_DRIVER_SERVICE);

    public ServiceCreateValidationFilter(ObjectManager objectManager, ObjectProcessManager processManager, StorageService storageService, JsonMapper jsonMapper,
            RevisionManager revisionManager) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.storageService = storageService;
        this.jsonMapper = jsonMapper;
        this.revisionManager = revisionManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);

        type = setKind(type, service, request);

        validateStack(service);

        validateSelector(request);

        validateMetadata(request);

        validateLaunchConfigs(service, request);

        validateNetworMode(service, request);

        validateIpsHostName(request);

        request = validateAndSetImage(request, service, type);

        validatePorts(service, type, request);

        request = setLBServiceEnvVarsAndHealthcheck(type, service, request);

        validateLbConfig(request, type);

        Object svcObj = super.create(type, request, next);
        if (!(svcObj instanceof Service)) {
            return svcObj;
        }

        revisionManager.createInitialRevision((Service) svcObj, CollectionUtils.toMap(request.getRequestObject()));

        return svcObj;
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        Service service = objectManager.loadResource(Service.class, id);

        validateLaunchConfigs(service, request);
        validateNetworMode(service, request);
        validateSelector(request);
        validateLbConfig(request, type);
        validatePorts(service, type, request);

        RevisionDiffomatic diff = revisionManager.createNewRevision(request.getSchemaFactory(),
                service,
                CollectionUtils.toMap(request.getRequestObject()));
        request.setRequestObject(diff.getNewRevisionData());

        Object result = super.update(type, id, request, next);
        if (result instanceof Service) {
            service = (Service)result;
            service = revisionManager.assignRevision(diff, service);
            processManager.update(service, null);
            return objectManager.reload(service);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public String setKind(String type, Service service, ApiRequest request) {
        // type is set via API request
        if (!ServiceConstants.KIND_SERVICE.equalsIgnoreCase(type)) {
            return type;
        }
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_EXTERNALIPS) != null || data.get(ServiceConstants.FIELD_HOSTNAME) != null) {
            return ServiceConstants.KIND_EXTERNAL_SERVICE;
        } else if (data.get(ServiceConstants.FIELD_STORAGE_DRIVER) != null) {
            return ServiceConstants.KIND_STORAGE_DRIVER_SERVICE;
        } else if (data.get(ServiceConstants.FIELD_NETWORK_DRIVER) != null) {
            return ServiceConstants.KIND_NETWORK_DRIVER_SERVICE;
        } else if (data.get(ServiceConstants.FIELD_SELECTOR_CONTAINER) != null) {
            return ServiceConstants.KIND_SELECTOR_SERVICE;
        } else if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> lbConfig = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_LB_CONFIG,
                    Map.class);
            if (lbConfig != null && lbConfig.containsKey(ServiceConstants.FIELD_PORT_RULES)) {
                List<PortRule> portRules = jsonMapper.convertCollectionValue(
                        lbConfig.get(ServiceConstants.FIELD_PORT_RULES), List.class, PortRule.class);
                for (PortRule rule : portRules) {
                    if (rule.getSourcePort() != null && rule.getSourcePort().longValue() > 0) {
                        return ServiceConstants.KIND_LOAD_BALANCER_SERVICE;
                    }
                }
            }
            Map<String, Object> launchConfig = DataAccessor.getFieldFromRequest(request,
                    ServiceConstants.FIELD_LAUNCH_CONFIG,
                    Map.class);
            if (ServiceConstants.IMAGE_DNS.equals(launchConfig
                    .get(InstanceConstants.FIELD_IMAGE))) {
                return ServiceConstants.KIND_DNS_SERVICE;
            }
            if (data.get(ServiceConstants.FIELD_SCALE) != null) {
                return ServiceConstants.KIND_SCALING_GROUP_SERVICE;
            }
        }
        return type;
    }

    @SuppressWarnings("unchecked")
    public void validateLbConfig(ApiRequest request, String type) {
        // add lb information to the metadata
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }
        Map<String, Object> lbConfig = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_LB_CONFIG,
                Map.class);
        if (lbConfig != null && lbConfig.containsKey(ServiceConstants.FIELD_PORT_RULES)) {
            List<PortRule> portRules = jsonMapper.convertCollectionValue(
                    lbConfig.get(ServiceConstants.FIELD_PORT_RULES), List.class, PortRule.class);
            for (PortRule rule : portRules) {
                // either serviceId or instanceId or selector are required
                boolean emptyService = rule.getServiceId() == null;
                boolean emptyInstance = rule.getInstanceId() == null;
                boolean emptySelector = StringUtils.isEmpty(rule.getSelector());
                int count = 0;
                count = !emptySelector ? ++count : count;
                count = !emptyService ? ++count : count;
                count = !emptyInstance ? ++count : count;

                if (count == 0) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED,
                            "serviceId");
                }
                if (count > 1) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                            "Can't specify both selector and serviceId");
                }

                if (emptySelector && rule.getTargetPort() == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "targetPort");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public ApiRequest setLBServiceEnvVarsAndHealthcheck(String type, Service lbService, ApiRequest request) {
        if (!ServiceConstants.KIND_LOAD_BALANCER_SERVICE.equalsIgnoreCase(type)) {
            return request;
        }

        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) == null) {
            return request;
        }

        Map<Object, Object> launchConfig = (Map<Object, Object>) data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);

        ServiceUtil.injectBalancerLabelsAndHealthcheck(launchConfig);
        data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, launchConfig);
        request.setRequestObject(data);
        return request;
    }

    public void validatePorts(Service service, String type, ApiRequest request) {
        validateLBPortRules(type, request);
        validatePorts(type, request);
    }

    @SuppressWarnings("unchecked")
    private void validatePorts(String type, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) == null) {
            return;
        }
        Map<String, Object> lc = (Map<String, Object>) data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        List<String> ports = jsonMapper.convertCollectionValue(
                lc.get(InstanceConstants.FIELD_PORTS), List.class, String.class);
        if (ports != null) {
            List<String> normalized = new ArrayList<>();
            for (Object port : ports) {
                if (port == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED,
                            InstanceConstants.FIELD_PORTS);
                }
                /* This will parse the PortSpec and throw an error */
                PortSpec spec = new PortSpec(port.toString());
                if (type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE) && spec.getPublicPort() == null) {
                    spec.setPublicPort(spec.getPrivatePort());
                }
                normalized.add(spec.toSpec());
            }
            lc.put(InstanceConstants.FIELD_PORTS, normalized);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateLBPortRules(String type, ApiRequest request) {
        if (!type.equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)) {
            return;
        }
        Map<String, Object> lbConfig = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_LB_CONFIG,
                Map.class);
        if (lbConfig == null) {
            return;
        }

        if (lbConfig != null && lbConfig.containsKey(ServiceConstants.FIELD_PORT_RULES)) {
            List<PortRule> portRules = jsonMapper.convertCollectionValue(
                    lbConfig.get(ServiceConstants.FIELD_PORT_RULES), List.class, PortRule.class);
            for (PortRule portRule : portRules) {
                if (portRule.getSourcePort() != null && portRule.getSourcePort().equals(LB_HEALTH_CHECK_PORT)) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                            "Port " + LB_HEALTH_CHECK_PORT + " is reserved for service health check");
                }
            }
        }
    }

    protected void validateSelector(ApiRequest request) {
        String selectorContainer = DataAccessor.getFieldFromRequest(request,
                ServiceConstants.FIELD_SELECTOR_CONTAINER,
                String.class);
        if (selectorContainer != null) {
            SelectorUtils.getSelectorConstraints(selectorContainer);
        }
    }

    protected void validateMetadata(ApiRequest request) {

        Object metadata = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_METADATA,
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
        List<String> invalidStates = Arrays.asList(CommonStatesConstants.ERROR, CommonStatesConstants.REMOVED,
                CommonStatesConstants.REMOVING);
        if (env == null || invalidStates.contains(env.getState())) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE, InstanceConstants.FIELD_STACK_ID);
        }
    }

    @SuppressWarnings("unchecked")
    protected ApiRequest validateAndSetImage(ApiRequest request, Service service, String type) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        String image = null;
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            Map<String, Object> launchConfig = (Map<String, Object>) data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
            image = DataAccessor.fromMap(launchConfig).withKey(InstanceConstants.FIELD_IMAGE).as(String.class);
            if (image == null) {
                // for storage driver and network driver services, set image to null
                if (DRIVER_TYPES.contains(type)) {
                    data.remove(ServiceConstants.FIELD_LAUNCH_CONFIG);
                }
            } else {
                storageService.validateImageAndSetImage(launchConfig, true);
            }
        }

        Object selector = data.get(ServiceConstants.FIELD_SELECTOR_CONTAINER);
        boolean isSelector = selector != null && !selector.toString().isEmpty();
        if (!isSelector && data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            if (image == null || image.toString().equalsIgnoreCase(ServiceConstants.IMAGE_NONE)) {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                        "Image is required when " + ServiceConstants.FIELD_SELECTOR_CONTAINER
                                + " is not passed in");
            }
        }

        List<Object> modifiedSlcs = new ArrayList<>();
        if (data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
           List<Object> slcs = (List<Object>)data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
           for (Object slcObj : slcs) {
                Map<String, Object> slc = (Map<String, Object>) slcObj;
                storageService.validateImageAndSetImage(slcObj, true);
                modifiedSlcs.add(slc);
            }

            data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, modifiedSlcs);
        }

        request.setRequestObject(data);
        return request;
    }

    protected void validateIpsHostName(ApiRequest request) {

        List<?> externalIps = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_EXTERNALIPS,
                List.class);

        String hostName = DataAccessor.getFieldFromRequest(request, ServiceConstants.FIELD_HOSTNAME,
                String.class);

        boolean isExternalIps = externalIps != null && !externalIps.isEmpty();
        boolean isHostName = hostName != null && !hostName.isEmpty();

        if (isExternalIps && isHostName) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                    ServiceConstants.FIELD_EXTERNALIPS + " and "
                            + ServiceConstants.FIELD_HOSTNAME + " are mutually exclusive");
        }
    }

    @SuppressWarnings("unchecked")
    protected void validateNetworMode(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());

        Object primaryLaunchConfigObj = data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        if (primaryLaunchConfigObj == null) {
            return;
        }
        String primaryNetworkMode = NetworkConstants.NETWORK_MODE_MANAGED;
        Map<String, Object> primaryLC = (Map<String, Object>) primaryLaunchConfigObj;
        if (primaryLC.get(InstanceConstants.FIELD_NETWORK_MODE) != null) {
            primaryNetworkMode = primaryLC.get(InstanceConstants.FIELD_NETWORK_MODE).toString();
        }

        List<Object> modifiedSlcs = new ArrayList<>();
        if (data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
            List<Object> slcs = (List<Object>) data.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS);
            for (Object slcObj : slcs) {
                Map<String, Object> slc = (Map<String, Object>) slcObj;
                String secondaryNetworkMode = NetworkConstants.NETWORK_MODE_CONTAINER;
                if (primaryNetworkMode.equalsIgnoreCase(NetworkConstants.NETWORK_MODE_HOST)
                        || primaryNetworkMode.equalsIgnoreCase(NetworkConstants.NETWORK_MODE_NONE)) {
                    secondaryNetworkMode = primaryNetworkMode;
                } else {
                    slc.put(InstanceConstants.FIELD_NETWORK_CONTAINER_ID, service.getName());
                }
                slc.put(InstanceConstants.FIELD_NETWORK_MODE, secondaryNetworkMode);
                modifiedSlcs.add(slc);
            }
            data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, modifiedSlcs);
            request.setRequestObject(data);
        }
    }

    protected void validateLaunchConfigs(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object newName = data.get("name");
        String serviceName = newName != null ? newName.toString() : service.getName();
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        validateLaunchConfigNames(service, serviceName, launchConfigs);
        validateLaunchConfigsCircularRefs(service, serviceName, launchConfigs);
        validateScale(service, request);
    }

    protected void validateScale(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());

        Long scaleMin = DataAccessor.fieldLong(service, ServiceConstants.FIELD_SCALE_MIN);
        if (data.get(ServiceConstants.FIELD_SCALE_MIN) != null) {
            scaleMin = Long.valueOf(data.get(ServiceConstants.FIELD_SCALE_MIN).toString());
        }

        Long scaleMax = DataAccessor.fieldLong(service, ServiceConstants.FIELD_SCALE_MAX);
        if (data.get(ServiceConstants.FIELD_SCALE_MAX) != null) {
            scaleMax = Long.valueOf(data.get(ServiceConstants.FIELD_SCALE_MAX).toString());
        }

        if (scaleMax != null && scaleMin != null) {
            if (scaleMax.longValue() < scaleMin.longValue()) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        "ScaleMin can not be greater than scaleMax");
            }
        }

        Object newLaunchConfig = data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        if (newLaunchConfig == null) {
            return;
        }

        Object launchConfig = DataAccessor.field(service, ServiceConstants.FIELD_LAUNCH_CONFIG,
                Object.class);

        if (launchConfig == null) {
            return;
        }
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
            validateLaunchConfigCircularRef(launchConfigName, launchConfigRefs, new HashSet<>());
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
                    .get(InstanceConstants.FIELD_NETWORK_CONTAINER_ID);
            if (networkFromLaunchConfig != null) {
                refs.add((String) networkFromLaunchConfig);
            }
            Object volumesFromLaunchConfigs = launchConfig
                    .get(InstanceConstants.FIELD_VOLUMES_FROM);
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
            for (String usedLcName : ServiceUtil.getLaunchConfigNames(existingSvc)) {
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
       ServiceUtil.validateDNSPatternForName(name);
    }
}
