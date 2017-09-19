package io.cattle.platform.core.addon;

import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Volume;
import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.annotation.Type;

import java.util.List;

@Type(list = false)
public class DeploymentSyncRequest {

    String deploymentUnitUuid;
    String revision;

    Long clusterId;
    String namespace;
    String nodeName;

    String externalId;
    List<Instance> containers;
    List<Volume> volumes;
    List<Credential> registryCredentials;
    List<Network> networks;

    public DeploymentSyncRequest() {
    }

    public DeploymentSyncRequest(DeploymentUnit unit, String nodeName, String namespace, String revision, List<Instance> containers, List<Volume> volumes,
                                 List<Credential> registryCredentials, List<Network> networks, Long clusterId) {
        this.deploymentUnitUuid = unit == null ? null : unit.getUuid();
        this.externalId = unit == null ? null : unit.getExternalId();
        this.namespace = namespace;
        this.nodeName = nodeName;
        this.revision = revision;
        this.containers = containers;
        this.volumes = volumes;
        this.registryCredentials = registryCredentials;
        this.networks = networks;
        this.clusterId = clusterId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getExternalId() {
        return externalId;
    }


    public String getDeploymentUnitUuid() {
        return deploymentUnitUuid;
    }

    @Field(typeString = "reference[cluster]")
    public Long getClusterId() {
        return clusterId;
    }

    public void setDeploymentUnitUuid(String deploymentUnitUuid) {
        this.deploymentUnitUuid = deploymentUnitUuid;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Field(typeString = "array[container]")
    public List<Instance> getContainers() {
        return containers;
    }

    public void setContainers(List<Instance> containers) {
        this.containers = containers;
    }

    public List<Volume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<Volume> volumes) {
        this.volumes = volumes;
    }

    public List<Credential> getRegistryCredentials() {
        return registryCredentials;
    }

    public void setRegistryCredentials(List<Credential> registryCredentials) {
        this.registryCredentials = registryCredentials;
    }

    public List<Network> getNetworks() {
        return networks;
    }

    public void setNetworks(List<Network> networks) {
        this.networks = networks;
    }

    public String getNodeName() {
        return nodeName;
    }

}
