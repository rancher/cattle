package io.cattle.platform.servicediscovery.deployment.impl.unit;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicStringProperty;

/*
 * Need this class as LB service ports are being processed differently
 */
public class LoadBalancerDeploymentUnitInstance extends DefaultDeploymentUnitInstance {
    private static final DynamicStringProperty DEFAULT_LB_IMAGE_UUID = ArchaiusUtil
            .getString("lb.instance.image");

    public LoadBalancerDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Instance instance, String launchConfigName) {
        super(context, uuid, service, instanceName, instance, launchConfigName);
    }

    protected Map<String, Object> populateLaunchConfigData(Map<String, Object> deployParams) {
        Map<String, Object> launchConfigData = super.populateLaunchConfigData(deployParams);
        if (ServiceDiscoveryUtil.isV1LB(service.getKind(), launchConfigData)) {
            setPorts(launchConfigData);
            setImage(launchConfigData);
        }
        return launchConfigData;
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
                // private port of LB instance = public port. Original private port specified,
                // is set on lb target in config backend section
                // protocol is always tcp
                spec.setPrivatePort(spec.getPublicPort());
                String fullPort = spec.toSpec();
                newPorts.add(fullPort);
            }
            launchConfigData.put(InstanceConstants.FIELD_PORTS, newPorts);
        }
    }
    
    protected void setImage(Map<String, Object> launchConfigData) {
        if (launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID) == null) {
            launchConfigData.put(InstanceConstants.FIELD_IMAGE_UUID, "docker:" + DEFAULT_LB_IMAGE_UUID.get());
        }
    }
}