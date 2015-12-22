package io.cattle.platform.servicediscovery.deployment.impl.unit;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants.SystemContainer;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicStringProperty;

public class LoadBalancerDeploymentUnitInstance extends DefaultDeploymentUnitInstance {
    private static final DynamicStringProperty DEFAULT_LB_IMAGE_UUID = ArchaiusUtil
            .getString("agent.instance.image.uuid");

    public LoadBalancerDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Instance instance, Map<String, String> labels, String launchConfigName) {
        super(context, uuid, service, instanceName, instance, labels, launchConfigName);
    }

    protected Map<String, Object> populateLaunchConfigData(Map<String, Object> deployParams) {
        Map<String, Object> launchConfigData = super.populateLaunchConfigData(deployParams);
        setLabels(launchConfigData);
        setSystemContainer(launchConfigData);
        setPorts(launchConfigData);
        return launchConfigData;
    }

    protected void setSystemContainer(Map<String, Object> launchConfigData) {
        launchConfigData.put(InstanceConstants.FIELD_PRIVILEGED, "true");
        launchConfigData.put(InstanceConstants.FIELD_SYSTEM_CONTAINER, SystemContainer.LoadBalancerAgent);
    }

    @SuppressWarnings("unchecked")
    protected void setLabels(Map<String, Object> launchConfigData) {
        Object labels = launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels == null) {
            labels = new HashMap<String, String>();
        }
        ((HashMap<String, String>) labels).put(SystemLabels.LABEL_AGENT_ROLE, SystemContainer.LoadBalancerAgent.name());
        ((HashMap<String, String>) labels).put(SystemLabels.LABEL_AGENT_CREATE, "true");
        ((HashMap<String, String>) labels).put(SystemLabels.LABEL_AGENT_URI_PREFIX, "delegate");
        launchConfigData.put(InstanceConstants.FIELD_LABELS, labels);
        if (launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID) == null) {
            launchConfigData.put(InstanceConstants.FIELD_IMAGE_UUID, DEFAULT_LB_IMAGE_UUID.get());
        }
    }

    @SuppressWarnings("unchecked")
    protected void setPorts(Map<String, Object> launchConfigData) {
        List<String> ports = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        List<String> newPorts = new ArrayList<>();
        if (ports != null) {
            for (String port : ports) {
                PortSpec spec = new PortSpec(port);
                if (spec.getPublicPort() == null) {
                    spec.setPublicPort(spec.getPrivatePort());
                }
                String fullPort = spec.getPublicPort().toString() + ":" + spec.getPublicPort().toString();
                newPorts.add(fullPort);
            }
            launchConfigData.put(InstanceConstants.FIELD_PORTS, newPorts);
        }
    }
}