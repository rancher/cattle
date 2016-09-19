package io.cattle.platform.allocator.util;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.allocator.service.DiskInfo;
import io.cattle.platform.allocator.service.HostInfo;
import io.cattle.platform.allocator.service.InstanceInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class AllocatorUtils {

    public static final Set<String> UNMANGED_STORAGE_POOLS = new HashSet<String>(Arrays.asList(new String[]{"docker", "sim"}));

    public static Map<Pair<String, Long>, DiskInfo> allocateDiskForVolumes(long hostId, Instance instance,
            ObjectManager objectManager) {
        CacheManager cm = CacheManager.getCacheManagerInstance(objectManager);
        HostInfo hostInfo = cm.getHostInfo(hostId, true);
        InstanceInfo instanceInfo = hostInfo.getInstanceInfo(instance.getId());
        if (instanceInfo == null) {
            instanceInfo = new InstanceInfo(instance.getId(), hostId);
            hostInfo.addInstance(instanceInfo);
        }
        @SuppressWarnings("unchecked")
        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS)
                .as(Map.class);
        if (labels == null) {
            return null;
        }
        ArrayList<Pair<String, Long>> volumeList = new ArrayList<Pair<String, Long>>();
        for (Map.Entry<String, String> labelEntry : labels.entrySet()) {
            String labelKey = labelEntry.getKey();
            String labelPrefix = SystemLabels.LABEL_SCHEDULER_DISKSIZE_PREFIX;
            if (labelKey.startsWith(labelPrefix) && labelKey.length() > labelPrefix.length()) {
                String key = labelKey.substring(SystemLabels.LABEL_SCHEDULER_DISKSIZE_PREFIX.length());
                String labelValue = labelEntry.getValue();
                Long reserveSize = Long.parseLong(labelValue.replaceAll("[^0-9]", ""));
                volumeList.add(new ImmutablePair<String, Long>(key, reserveSize));
            }
        }
        if (volumeList.size() == 0) {
            // there is no disksize label really
            return null;
        }

        // figure out how to allocate volume requirements to disks with
        // different sizes: sort the allocating volumes by sizes(descending) and
        // sort the available disks by free space in descending order as well,
        // then loop through disks to try to allocate biggest requirement volume
        // first, so we can fail fast. If one volume is allocated, then move to
        // next for this same disk.

        // sort by required volume size
        Collections.sort(volumeList, new Comparator<Pair<String, Long>>() {
            @Override
            public int compare(Pair<String, Long> v1, Pair<String, Long> v2) {
                return v1.getRight() >= v2.getRight() ? -1 : 1;
            }
        });

        // load disk info and free size into a temporary list
        List<Pair<DiskInfo, Long>> disks = new ArrayList<Pair<DiskInfo, Long>>();
        for (Entry<String, DiskInfo> entry : hostInfo.getAllDiskInfo()) {
            DiskInfo diskInfo = entry.getValue();
            Long capacity = diskInfo.getCapacity();
            Long allocatedSize = diskInfo.getAllocatedSize();
            Long freeSize = capacity - allocatedSize;
            disks.add(new MutablePair<DiskInfo, Long>(diskInfo, freeSize));
        }
        // sort by disk free size in descending order
        Collections.sort(disks, new Comparator<Pair<DiskInfo, Long>>() {
            @Override
            public int compare(Pair<DiskInfo, Long> v1, Pair<DiskInfo, Long> v2) {
                return v1.getRight() >= v2.getRight() ? -1 : 1;
            }
        });

        Map<Pair<String, Long>, DiskInfo> mapping = new HashMap<Pair<String, Long>, DiskInfo>();
        for (Pair<DiskInfo, Long> disk : disks) {
            // try to allocate as many volumes on one disk as possible
            int originalSize = volumeList.size();
            for (int i = 0; i < volumeList.size(); i++) {
                Pair<String, Long> vol = volumeList.get(i);
                if (vol.getRight() <= disk.getRight()) {
                    mapping.put(vol, disk.getLeft());
                    disk.setValue(disk.getRight() - vol.getRight());
                    volumeList.remove(vol);
                    i--;
                    continue;
                } else {
                    // if there is no change in number of volumeList, then it
                    // means no other disks could accommodate the largest of the
                    // rest volumes
                    if (volumeList.size() == originalSize) {
                        return null;
                    }
                    break;
                }
            }
            if (volumeList.size() == 0) {
                return mapping;
            }
        }
        return null;
    }
}
