package io.cattle.platform.core.addon;

import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list = false)
public class K8sConfig {

    List<String> admissionControllers;
    String serviceNetCidr;
    String kubeConfig;

    public List<String> getAdmissionControllers() {
        return admissionControllers;
    }

    public void setAdmissionControllers(List<String> admissionControllers) {
        this.admissionControllers = admissionControllers;
    }

    public String getServiceNetCidr() {
        return serviceNetCidr;
    }

    public void setServiceNetCidr(String serviceNetCidr) {
        this.serviceNetCidr = serviceNetCidr;
    }

    public String getKubeConfig() {
        return kubeConfig;
    }

    public void setKubeConfig(String kubeConfig) {
        this.kubeConfig = kubeConfig;
    }
}
