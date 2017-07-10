package io.cattle.platform.api.serviceproxy;

import io.cattle.platform.api.instance.ContainerProxyActionHandler;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class ServiceProxyManager extends AbstractNoOpResourceManager {

    ServiceDao serviceDao;
    ContainerProxyActionHandler actionHandler;
    ObjectManager objectManager;

    public ServiceProxyManager(ServiceDao serviceDao, ContainerProxyActionHandler actionHandler, ObjectManager objectManager) {
        super();
        this.serviceDao = serviceDao;
        this.actionHandler = actionHandler;
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        ServiceProxy proxy = request.proxyRequestObject(ServiceProxy.class);
        String serviceName = proxy.getService();
        if (StringUtils.isBlank(serviceName)) {
            request.setResponseCode(ResponseCodes.NOT_FOUND);
            return null;
        }

        String[] parts = StringUtils.split(serviceName, ".", 2);
        Service service = null;

        if (parts.length == 2) {
            service = serviceDao.findServiceByName(ApiUtils.getPolicy().getAccountId(), parts[1], parts[0]);
        } else {
            service = serviceDao.findServiceByName(ApiUtils.getPolicy().getAccountId(), parts[0]);
        }

        if (service != null) {
            List<Long> instanceIds = DataAccessor.fieldLongList(service, ServiceConstants.FIELD_INSTANCE_IDS);
            if (instanceIds.size() > 0) {
                return actionHandler.perform(null, objectManager.loadResource(Instance.class, instanceIds.get(0)), request);
            }
        }

        request.setResponseCode(ResponseCodes.NOT_FOUND);
        return null;
    }

}
