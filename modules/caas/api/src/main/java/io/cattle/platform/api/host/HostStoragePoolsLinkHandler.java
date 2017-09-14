package io.cattle.platform.api.host;

import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;

import java.io.IOException;

import static io.cattle.platform.core.constants.HostConstants.*;

public class HostStoragePoolsLinkHandler implements LinkHandler {

    ObjectManager objectManager;
    StoragePoolDao storagePoolDao;

    public HostStoragePoolsLinkHandler(ObjectManager objectManager, StoragePoolDao storagePoolDao) {
        this.objectManager = objectManager;
        this.storagePoolDao = storagePoolDao;
    }
    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return STORAGE_POOLS_LINK.equalsIgnoreCase(link);
    }

    @Override
    public Object link(String name, Object obj, ApiRequest request) throws IOException {
        if (obj instanceof Host) {
            Host host = (Host) obj;
            return storagePoolDao.findPoolsForHost(((Host) obj).getId());
        }

        return null;
    }
}
