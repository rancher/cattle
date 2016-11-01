package io.cattle.platform.servicediscovery.api.filter;


import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class ServiceStackStorageDriverFilter extends AbstractDefaultResourceManagerFilter {

    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList(
            StandardProcess.REMOVE.toString().toLowerCase(),
            StandardProcess.DEACTIVATE.toString().toLowerCase(),
            ServiceConstants.ACTION_STACK_DEACTIVATE_SERVICES
            ));

    @Inject
    StoragePoolDao storagePoolDao;
    @Inject
    ObjectManager objectManager;

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        validateInUse(type, id, request, next);
        return super.delete(type, id, request, next);
    }

    protected void validateInUse(String type, String id, ApiRequest request, ResourceManager next) {
        Object resource = getById(type, id, new ListOptions(), next);
        List<Service> services = new ArrayList<>();
        if (resource instanceof Service) {
            services.add((Service)resource);
        } else if (resource instanceof Stack) {
            services = objectManager.find(Service.class,
                    SERVICE.STACK_ID, ((Stack) resource).getId(),
                    SERVICE.REMOVED, null);
        }

        for (Service service : services) {
            List<Long> ids = storagePoolDao.findVolumesInUseByServiceDriver(service.getId());
            if (ids.size() > 0) {
                throwException(ids);
            }
        }
    }

    protected void throwException(List<Long> ids) {
        IdFormatter idF = ApiContext.getContext().getIdFormatter();
        List<Object> stringIds = new ArrayList<>(ids.size());
        for (Long volumeId : ids) {
            stringIds.add(idF.formatId(VolumeConstants.TYPE, volumeId));
        }
        throw new ClientVisibleException(ResponseCodes.CONFLICT, "DRIVER_IN_USE", "Driver from service is in use by volumes",
                stringIds.toString());
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (ACTIONS.contains(request.getAction())) {
            validateInUse(type, request.getId(), request, next);
        }
        return super.resourceAction(type, request, next);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Service.class, Stack.class};
    }

    @Override
    public String[] getTypes() {
        return new String[] { ServiceConstants.KIND_STORAGE_DRIVER_SERVICE };
    }

}
