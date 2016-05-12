package io.cattle.platform.allocator.service;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheManager
{
    private static final Logger log = LoggerFactory.getLogger(CacheManager.class);

    // singleton
    private static CacheManager instance = null;
    private final Map<Long, HostInfo> hostsInfo = new HashMap<Long, HostInfo>();
    private ObjectManager objectManager;

    protected CacheManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public static synchronized CacheManager getCacheManagerInstance(ObjectManager objectManager) {
        if (instance == null) {
            instance = new CacheManager(objectManager);
        }
        return instance;
    }

    public synchronized HostInfo getHostInfo(Long hostId, boolean create) {
        HostInfo hostInfo = this.hostsInfo.get(hostId);
        if (hostInfo == null && create) {
            hostInfo = loadHostInfoToCache(hostId);

            // now load existing instances info that consumes host resources(disk etc) into hostInfo
            hostInfo.loadAllocatedInstanceResource(this.objectManager);
        }

        return hostInfo;
    }

    private HostInfo loadHostInfoToCache(Long hostId) {
        HostInfo hostInfo = new HostInfo(hostId);
        
        // we need to cache all host info related to disks first
        Host host = objectManager.loadResource(Host.class, hostId);
        if (host == null) {
            return hostInfo;
        }
        
        // load all raw disks info
        Object obj = DataAccessor.fields(host).withKey(HostConstants.FIELD_INFO).get();

        @SuppressWarnings("unchecked")
        Map<Object, Object> mountPoints = (Map<Object, Object>) CollectionUtils.getNestedValue(obj, "diskInfo",
                "mountPoints");

        // if this host does not have any mountPoints in the case of simulator
        if (mountPoints == null) {
            return hostInfo;
        }
        for (Entry<Object, Object> mp : mountPoints.entrySet()) {
            Double totalDouble = (Double)CollectionUtils.getNestedValue(mp.getValue(), "total");
            if (totalDouble == null) {
                continue;
            }

            // round down from MB to GB in integer format
            Long total = Math.round(totalDouble / 1024);
            Double usedDouble = (Double)CollectionUtils.getNestedValue(mp.getValue(), "used");
            if (usedDouble == null) {
                continue;
            }

            // round down from MB to GB in integer format
            Long used = Math.round(usedDouble / 1024);
            DiskInfo diskInfo = new DiskInfo((String)mp.getKey(), total, used);
            hostInfo.addDisk(diskInfo);

            log.debug("Host [{}] has a mount point[{}, total:{}, used:{}]", hostId, (String) mp.getKey(), total, used);
        }
        this.hostsInfo.put(hostId, hostInfo);
        log.debug("added host [{}] information into cache manager", hostId);

        return hostInfo;
    }

    public synchronized void removeHostInfo(Long hostId) {
        if (this.hostsInfo.remove(hostId) != null) {
            log.debug("removed host [{}] information from cache manager", hostId);
        }
    }

}
