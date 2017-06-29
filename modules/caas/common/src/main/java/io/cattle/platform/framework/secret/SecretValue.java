package io.cattle.platform.framework.secret;

import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.model.Secret;

import org.apache.commons.lang3.StringUtils;

public class SecretValue {

    String name, uid, gid, mode, rewrapText;

    public SecretValue() {
    }

    public SecretValue(SecretReference ref, Secret secret, String value) {
        this.name = ref.getName();
        if (StringUtils.isBlank(this.name)) {
            this.name = secret.getName();
        }
        this.uid = ref.getUid();
        this.gid = ref.getGid();
        this.mode = ref.getMode();
        this.rewrapText = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRewrapText() {
        return rewrapText;
    }

    public void setRewrapText(String rewrapText) {
        this.rewrapText = rewrapText;
    }
}
