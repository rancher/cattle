package io.cattle.platform.docker.api.container;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.api.ContainerProxyActionHandler;
import io.cattle.platform.docker.api.model.ServiceProxy;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class ServiceProxyManager extends AbstractNoOpResourceManager {

    @Inject
    InstanceDao instanceDao;
    @Inject
    ContainerProxyActionHandler actionHandler;

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        ServiceProxy proxy = request.proxyRequestObject(ServiceProxy.class);
        String service = proxy.getService();
        String[] parts = StringUtils.split(service, ".", 2);
        List<? extends Instance> instances = null;

        if (parts.length == 2) {
            instances = instanceDao.findInstanceByServiceName(ApiUtils.getPolicy().getAccountId(), parts[1], parts[0]);
        } else {
            instances = instanceDao.findInstanceByServiceName(ApiUtils.getPolicy().getAccountId(), parts[0]);
        }

        if (instances.size() == 0) {
            request.setResponseCode(ResponseCodes.NOT_FOUND);
            return null;
        }

        return actionHandler.perform(null, instances.get(0), request);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ServiceProxy.class };
    }

}
