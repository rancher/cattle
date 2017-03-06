package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.lock.ServiceDiscoveryServiceSetLinksLock;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class SetServiceLinksActionHandler implements ActionHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    ServiceConsumeMapDao consumeMapDao;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectManager objMgr;
    @Inject
    IdFormatter idFormatter;


    @Override
    public String getName() {
        return ServiceConstants.PROCESS_SERVICE_SET_SERVICE_LINKS;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Service)) {
            return null;
        }
        final Service service = (Service) obj;

        final Map<String, ServiceLink> newServiceLinks = populateNewServiceLinks(request);

        validateLinks(newServiceLinks);
        if (newServiceLinks != null) {
            lockManager.lock(new ServiceDiscoveryServiceSetLinksLock(service), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    // remove old listeners set
                    removeOldServiceMaps(service, newServiceLinks);

                    // create a new set
                    createNewServiceMaps(service, newServiceLinks);
                }
            });
        }

        return service;
    }

    protected void validateLinks(final Map<String, ServiceLink> newServiceLinks) {
        for (ServiceLink link : newServiceLinks.values()) {
            Service targetService = objMgr.loadResource(Service.class, link.getServiceId());
            if (targetService == null || targetService.getRemoved() != null
                    || targetService.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                Object obfuscatedId = idFormatter.formatId("service", link.getServiceId());
                String obfuscatedIdStr = obfuscatedId != null ? obfuscatedId.toString() : null;
                String svcName = targetService != null ? targetService.getName() : obfuscatedIdStr;
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_REFERENCE,
                        "Service " + svcName + " is removed");
            }
        }
    }

    protected Map<String, ServiceLink> populateNewServiceLinks(ApiRequest request) {
        Map<String, ServiceLink> newServiceLinks = new HashMap<>();
        List<? extends ServiceLink> serviceLinks = DataAccessor.fromMap(request.getRequestObject()).withKey(
                    ServiceConstants.FIELD_SERVICE_LINKS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, ServiceLink.class);

        if (serviceLinks != null) {
            for (ServiceLink serviceLink : serviceLinks) {
                newServiceLinks.put(serviceLink.getUuid(), serviceLink);
            }
        }

        return newServiceLinks;
    }

    private void createNewServiceMaps(Service service, Map<String, ServiceLink> newServiceLinks) {
        for (ServiceLink newServiceLink : newServiceLinks.values()) {
            consumeMapDao.createServiceLink(service, newServiceLink);
        }
    }

    private void removeOldServiceMaps(Service service, Map<String, ServiceLink> newServiceLinks) {
        List<? extends ServiceConsumeMap> existingMaps = consumeMapDao.findConsumedMapsToRemove(service.getId());
        List<ServiceLink> linksToRemove = new ArrayList<>();

        for (ServiceConsumeMap existingMap : existingMaps) {
            ServiceLink existingLink = new ServiceLink(existingMap.getConsumedServiceId(), existingMap.getName());
            if (!newServiceLinks.containsKey(existingLink.getUuid())) {
                linksToRemove.add(existingLink);
            }
        }

        for (ServiceLink linkToRemove : linksToRemove) {
            consumeMapDao.removeServiceLink(service, linkToRemove);
        }
    }
}
