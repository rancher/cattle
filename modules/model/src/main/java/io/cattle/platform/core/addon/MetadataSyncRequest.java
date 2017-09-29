package io.cattle.platform.core.addon;

import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.addon.metadata.MetadataObject;
import io.cattle.platform.core.constants.InstanceConstants;

import java.util.HashMap;
import java.util.Map;

public class MetadataSyncRequest {

    String generation;
    boolean full;
    Map<String, MetadataObject> removes = new HashMap<>();
    Map<String, MetadataObject> updates = new HashMap<>();

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public Map<String, MetadataObject> getRemoves() {
        return removes;
    }

    public void setRemoves(Map<String, MetadataObject> removes) {
        this.removes = removes;
    }

    public Map<String, MetadataObject> getUpdates() {
        return updates;
    }

    public void setUpdates(Map<String, MetadataObject> updates) {
        this.updates = updates;
    }

    public void putAllUpdates(Map<String, MetadataObject> objects) {
        for (MetadataObject object : objects.values()) {
            add(object);
        }
    }

    public void add(Object obj) {
        if (obj instanceof InstanceInfo) {
            InstanceInfo instance = (InstanceInfo)obj;
            if (instance.isHidden() && !InstanceConstants.isNativeKubernetesPOD(instance)) {
                return;
            }
        }

        if (obj instanceof MetadataObject) {
            updates.put(((MetadataObject) obj).getUuid(), (MetadataObject) obj);
        } else if (obj instanceof Removed) {
            MetadataObject metadataObject = ((Removed) obj).getRemoved();
            removes.put(metadataObject.getUuid(), metadataObject);
        }
    }

    public int size() {
        return removes.size() + updates.size();
    }

    public void putAll(MetadataSyncRequest request) {
        removes.putAll(request.removes);
        updates.putAll(request.updates);
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

}
