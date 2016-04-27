package io.cattle.platform.allocator.service;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CacheManager
{
    // singleton
    private static CacheManager instance = null;
    private final Map<Long, HostInfo> hostsInfo = new HashMap<Long, HostInfo>();
    private ObjectManager objectManager;

    protected CacheManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
     }
    
    public static CacheManager getCacheManagerInstance(ObjectManager objectManager) {
        if (instance == null) {
            instance = new CacheManager(objectManager);
        }
        return instance;
    }

    public HostInfo getHostInfo(Long hostId) {
        HostInfo hostInfo = this.hostsInfo.get(hostId);
        if (hostInfo == null) {
            hostInfo = loadHostInfoToCache(hostId);
        }

        return hostInfo;
    }

    public HostInfo loadHostInfoToCache(Long hostId) {
        HostInfo hostInfo = new HostInfo(hostId);
        
        // we need to cache all host info related to disks first
        Host host = objectManager.loadResource(Host.class, hostId);
        if (host == null) {
            return hostInfo;
        }
        
        // load all raw disks info
        Object obj = DataAccessor.fields(host).withKey(HostConstants.FIELD_INFO).get();

        @SuppressWarnings("unchecked")
        Map<Object, Object> mountPoints = (Map<Object, Object>) CollectionUtils.getNestedValue(obj, "diskInfo", "mountPoints");
        for (Entry<Object, Object> mp : mountPoints.entrySet()) {
            Double totalDouble = (Double)CollectionUtils.getNestedValue(mp.getValue(), "total");
            if (totalDouble == null) {
                continue;
            }
            Long total = Math.round(totalDouble);
            Double usedDouble = (Double)CollectionUtils.getNestedValue(mp.getValue(), "used");
            if (usedDouble == null) {
                continue;
            }
            Long used = Math.round(usedDouble);
            DiskInfo diskInfo = new DiskInfo((String)mp.getKey(), total, used);
            hostInfo.addDisk(diskInfo);
        }
        this.hostsInfo.put(hostId, hostInfo);
        return hostInfo;
    }

    public void removeHostInfo(Long hostId) {
        this.hostsInfo.remove(hostId);
    }

    public DiskInfo getDiskInfoForHost(Long hostId, String diskDevicePath) {
        return getHostInfo(hostId).getDiskInfo(diskDevicePath);
    }

    public InstanceInfo getInstanceInfoForHost(Long hostId, Long instanceID) {
        InstanceInfo instanceInfo = getHostInfo(hostId).getInstanceInfo(instanceID);
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo(instanceID, hostId);
            setInstanceInfoForHost(hostId, instanceInfo);
        }

        return instanceInfo;
    }

    private void setInstanceInfoForHost(Long hostId, InstanceInfo instanceInfo) {
        getHostInfo(hostId).addInstance(instanceInfo);
    }

}
