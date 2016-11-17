package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

@Type(list = false)
public class MountEntry {
    String type = "mountEntry";
    String volumeName, instanceName, path, permission;
    Object instanceId, volumeId;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Field(typeString="reference[instance]")
    public Object getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Object instanceId) {
        this.instanceId = instanceId;
    }

    @Field(typeString="reference[volume]")
    public Object getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(Object volumeId) {
        this.volumeId = volumeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}