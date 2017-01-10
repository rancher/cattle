package DeploymentUnitFactory;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.ServiceDeploymentUnit;
import io.cattle.platform.servicediscovery.deployment.impl.unit.StandaloneDeploymentUnit;

public class DeploymentUnitFactory {

    public static io.cattle.platform.servicediscovery.deployment.DeploymentUnit fetchDeploymentUnit(
            DeploymentUnit unit, DeploymentUnitManagerContext context) {
        if (unit.getServiceId() != null) {
            return new ServiceDeploymentUnit(unit, context);
        }
        return new StandaloneDeploymentUnit(unit, context);
    }
}
