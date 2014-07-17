package io.cattle.platform.iaas.api.ippool;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;

import javax.inject.Inject;

public class IpPoolAcquireActionHandler implements ActionHandler {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    IpAddressDao ipAddressDao;

    @Override
    public String getName() {
        return "ippool.acquire";
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if ( ! ( obj instanceof IpPool ) ) {
            return null;
        }

        IpPool pool = (IpPool)obj;
        IpAddress ipAddress = ipAddressDao.createIpAddressFromPool(pool, null);

        processManager.scheduleStandardProcess(StandardProcess.CREATE, ipAddress, new HashMap<String, Object>());

        return objectManager.reload(ipAddress);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public IpAddressDao getIpAddressDao() {
        return ipAddressDao;
    }

    @Inject
    public void setIpAddressDao(IpAddressDao ipAddressDao) {
        this.ipAddressDao = ipAddressDao;
    }

}