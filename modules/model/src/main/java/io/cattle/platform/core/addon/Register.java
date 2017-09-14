package io.cattle.platform.core.addon;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Credential;

public class Register {

    boolean simulated;
    String id;
    String key;
    String accessKey;
    String secretKey;
    String state;
    String orchestration;
    K8sClientConfig k8sClientConfig;

    public Register() {
    }

    public Register(String key, Credential cred) {
        this.state = CommonStatesConstants.CREATING;
        this.key = key;
        this.id = key;
        if (cred != null) {
            this.state = CommonStatesConstants.ACTIVE;
            this.accessKey = cred.getPublicValue();
            this.secretKey = cred.getSecretValue();
        }
    }

    public String getOrchestration() {
        return orchestration;
    }

    public void setOrchestration(String orchestration) {
        this.orchestration = orchestration;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public K8sClientConfig getK8sClientConfig() {
        return k8sClientConfig;
    }

    public void setK8sClientConfig(K8sClientConfig k8sClientConfig) {
        this.k8sClientConfig = k8sClientConfig;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
