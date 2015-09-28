package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.net.NetUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ServiceCreateValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ResourceManagerLocator locator;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    StorageService storageService;

    @Inject
    NetworkDao ntwkDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Service.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { "service", "loadBalancerService", "externalService" };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Service service = request.proxyRequestObject(Service.class);
        
        validateEnvironment(service);

        validateName(type, service);

        validateLaunchConfigs(service, request);

        validateIpsHostName(request);

        validateImage(request, service);

        validateRequestedVip(request);

        return super.create(type, request, next);
    }

    protected void validateEnvironment(Service service) {
        Environment env = objectManager.loadResource(Environment.class, service.getEnvironmentId());
        if (StringUtils.equals(env.getState(), CommonStatesConstants.ERROR)) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE, 
                    InstanceConstants.FIELD_ENVIRONMENT);
        }
    }
    
    protected void validateImage(ApiRequest request, Service service) {
        List<Map<String, Object>> launchConfigs = populateLaunchConfigs(service, request);
        for (Map<String, Object> launchConfig : launchConfigs) {
            Object imageUuid = launchConfig.get(InstanceConstants.FIELD_IMAGE_UUID);
            if (imageUuid != null) {
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
        Service service = request.proxyRequestObject(Service.class);

        validateName(type, service);
        validateLaunchConfigs(service, request);

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


    protected void validateName(String type, Service service) {
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

        if (service.getName() != null && (service.getName().startsWith("-") || service.getName().endsWith("-"))) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_CHARACTERS,
                    "name");
        }
    }
}
