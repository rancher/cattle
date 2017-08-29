package io.cattle.platform.api.service;


import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.cattle.platform.core.model.tables.ServiceTable.*;

public class ServiceStackNetworkDriverFilter extends AbstractValidationFilter {

    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            StandardProcess.REMOVE.toString().toLowerCase(),
            StandardProcess.DEACTIVATE.toString().toLowerCase(),
            ServiceConstants.ACTION_STACK_DEACTIVATE_SERVICES
            ));

    NetworkDao networkDao;
    ObjectManager objectManager;

    public ServiceStackNetworkDriverFilter(NetworkDao networkDao, ObjectManager objectManager) {
        super();
        this.networkDao = networkDao;
        this.objectManager = objectManager;
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        Object resource = getById(type, id, new ListOptions(), next);
        validateInUse(resource, request);
        return super.delete(type, id, request, next);
    }

    protected void validateInUse(Object resource, ApiRequest request) {
        List<Service> services = new ArrayList<>();
        if (resource instanceof Service) {
            services.add((Service)resource);
        } else if (resource instanceof Stack) {
            services = objectManager.find(Service.class,
                    SERVICE.STACK_ID, ((Stack) resource).getId(),
                    SERVICE.REMOVED, null);
        }

        List<Long> ids = networkDao.findInstancesInUseByServiceDriver(CollectionUtils.map(services, Service::getId));
        if (ids.size() > 0) {
            throwException(ids);
        }
    }

    protected void throwException(List<Long> ids) {
        IdFormatter idF = ApiContext.getContext().getIdFormatter();
        List<Object> stringIds = new ArrayList<>(ids.size());
        for (Long instanceId : ids) {
            stringIds.add(idF.formatId(InstanceConstants.TYPE, instanceId));
        }
        throw new ClientVisibleException(ResponseCodes.CONFLICT, "DRIVER_IN_USE", "Driver from service is in use by instances",
                StringUtils.join(stringIds, ","));
    }

    @Override
    public Object perform(Object obj, ApiRequest request, ActionHandler next) {
        if (ACTIONS.contains(request.getAction())) {
            validateInUse(obj, request);
        }
        return super.perform(obj, request, next);
    }

}
