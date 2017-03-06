package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.ServiceUtil.UpgradedConfig;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
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
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;
    @Inject
    StorageService storageService;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ServiceDao serviceDao;

    private static final int LB_HEALTH_CHECK_PORT = 42;

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

        request = validateAndSetImage(request, service, type);

        validatePorts(service, type, request);

        request = setServiceIndexStrategy(type, request);

        request = setLBServiceEnvVarsAndHealthcheck(type, service, request);

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
                // either serviceId or instanceId or selector are required
                boolean emptyService = StringUtils.isEmpty(rule.getServiceId());
                boolean emptyInstance = StringUtils.isEmpty(rule.getInstanceId());
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
        validatePorts(request);
    }

    @SuppressWarnings("unchecked")
    private void validatePorts(ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        if (data.get(ServiceConstants.FIELD_LAUNCH_CONFIG) == null) {
            return;
        }
        Map<String, Object> lc = (Map<String, Object>) data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        List<String> ports = jsonMapper.convertCollectionValue(
                lc.get(InstanceConstants.FIELD_PORTS), List.class, String.class);
        if (ports != null) {
            for (Object port : ports) {
                if (port == null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED,
                            InstanceConstants.FIELD_PORTS);
                }
                /* This will parse the PortSpec and throw an error */
                new PortSpec(port.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateLBPortRules(String type, ApiRequest request) {
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
                if (portRule.getSourcePort() != null && portRule.getSourcePort().equals(LB_HEALTH_CHECK_PORT)) {
                    throw new ValidationErrorException(ValidationErrorCodes.INVALID_OPTION,
                            "Port " + LB_HEALTH_CHECK_PORT + " is reserved for service health check");
                }
            }
        }
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
    protected ApiRequest validateAndSetImage(ApiRequest request, Service service, String type) {
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
        validatePorts(service, type, request);
        request = setForUpgrade(service, request);

        return super.update(type, id, request, next);
    }

    protected ApiRequest setForUpgrade(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        List<Map<String, Object>> secondary = new ArrayList<>();
        Map<String, Object> primary = null;
        for (Map<String, Object> lc : launchConfigs) {
            if (!lc.containsKey("name")) {
                primary = lc;
            } else {
                secondary.add(lc);
            }
        }
        UpgradedConfig upgrade = ServiceUtil.mergeLaunchConfigs(service, primary, secondary);
        if (upgrade == null) {
            return request;
        }
        if (upgrade.isRunUpgrade()) {
            InstanceRevision oldRevision = serviceDao.getCurrentRevision(service);
            if (oldRevision != null) {
                data.put(InstanceConstants.FIELD_PREVIOUS_REVISION_ID, oldRevision.getId());
            }
            InstanceRevision newRevision = serviceDao.createRevision(service, upgrade.getPrimaryLaunchConfig(),
                    upgrade.getSecondaryLaunchConfigs(), false);
            data.put(InstanceConstants.FIELD_REVISION_ID, newRevision.getId());
            data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, upgrade.getPrimaryLaunchConfig());
            data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, upgrade.getSecondaryLaunchConfigs());
            request.setRequestObject(data);
            setForUpgrade(service);
        }

        return request;
    }

    protected void setForUpgrade(Service service) {
        if (ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
            Map<String, Object> data = new HashMap<>();
            data.put(ServiceConstants.FIELD_IS_UPGRADE, true);
            objectManager.setFields(objectManager.reload(service), data);
        }
    }

    protected void validateLaunchConfigs(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object newName = data.get("name");
        String serviceName = newName != null ? newName.toString() : service.getName();
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        validateLaunchConfigNames(service, serviceName, launchConfigs);
        validateLaunchConfigsCircularRefs(service, serviceName, launchConfigs);
        validateLaunchConfigScale(service, request);
    }

    protected void validateLaunchConfigScale(Service service, ApiRequest request) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        Object newLaunchConfig = data.get(ServiceConstants.FIELD_LAUNCH_CONFIG);
        if (newLaunchConfig == null) {
            return;
        }

        Object launchConfig = DataAccessor.field(service, ServiceConstants.FIELD_LAUNCH_CONFIG,
                Object.class);

        if (launchConfig == null) {
            return;
        }

        ServiceUtil.validateScaleSwitch(newLaunchConfig, launchConfig);
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
       validateDNSPatternForName(name);
    }
}
