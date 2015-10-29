package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancerTargetsInfo {
    int uuid;
    LoadBalancerTargetPortSpec portSpec;
    List<LoadBalancerTargetInfo> targets;
    InstanceHealthCheck healthCheck;

    public LoadBalancerTargetsInfo(List<LoadBalancerTargetInfo> lbTargetInfo, int uuid) {
        this(lbTargetInfo, lbTargetInfo.isEmpty() ? null : lbTargetInfo.get(0).getPortSpec());
        this.uuid = uuid;
    }

    public LoadBalancerTargetsInfo(List<LoadBalancerTargetInfo> lbTargetInfo, LoadBalancerTargetPortSpec portSpec) {
        this.targets = lbTargetInfo;
        for (LoadBalancerTargetInfo target : this.targets) {
            if (target.getHealthCheck() != null) {
                this.healthCheck = target.getHealthCheck();
                break;
            }
        }
        this.portSpec = portSpec;
    }

    public LoadBalancerTargetsInfo(LoadBalancerTargetsInfo that) {
        this.uuid = that.getUuid();
        this.portSpec = that.getPortSpec();
        this.targets = that.getTargets();
        this.healthCheck = that.healthCheck;
    }

    public List<LoadBalancerTargetInfo> getTargets() {
        return targets;
    }

    public void setTargets(List<LoadBalancerTargetInfo> targets) {
        this.targets = targets;
    }

    public int getUuid() {
        return uuid;
    }

    public void setUuid(int uuid) {
        this.uuid = uuid;
    }

    public LoadBalancerTargetPortSpec getPortSpec() {
        return portSpec;
    }

    public void setPortSpec(LoadBalancerTargetPortSpec portSpec) {
        this.portSpec = portSpec;
    }

    public InstanceHealthCheck getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(InstanceHealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    public void addTargets(List<LoadBalancerTargetInfo> targets) {
        if (this.targets == null) {
            this.targets = new ArrayList<>();
        }
        this.targets.addAll(targets);
    }
}
