package io.cattle.platform.inator.factory;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.deploy.DeploymentUnitInator;
import io.cattle.platform.inator.deploy.ServiceInator;
import io.cattle.platform.inator.planner.impl.GlobalServicePlanner;
import io.cattle.platform.inator.planner.impl.ScaleServicePlanner;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InatorFactoryinator {

    @Inject
    InatorServices svc;

    public Inator buildInator(String type, Long id) {
        Class<?> clz = svc.objectManager.getSchemaFactory().getSchemaClass(type);
        if (ServiceConstants.KIND_SERVICE.equals(svc.objectManager.getType(clz))) {
            return buildServiceUnit(id);
        } else if (ServiceConstants.KIND_DEPLOYMENT_UNIT.equals(type)) {
            return buildDeploymentUnit(id);
        }

        return null;
    }

    private Inator buildServiceUnit(Long id) {
        Service service = svc.objectManager.loadResource(Service.class, id);
        if (service == null) {
            return null;
        }

        if (ServiceUtil.isGlobalService(service)) {
            return new ServiceInator(service, new GlobalServicePlanner(service, svc), svc);
        }

        return new ServiceInator(service, new ScaleServicePlanner(service, svc), svc);
    }

    private Inator buildDeploymentUnit(Long id) {
        DeploymentUnit unit = svc.objectManager.loadResource(DeploymentUnit.class, id);
        if (unit == null) {
            return null;
        }

        StackWrapper stack = new StackWrapper(svc.objectManager.loadResource(Stack.class, unit.getStackId()));
        Service service = svc.objectManager.loadResource(Service.class, unit.getServiceId());
        DeploymentUnitWrapper unitWrapper = new DeploymentUnitWrapper(unit, service, svc);
        return new DeploymentUnitInator(unitWrapper, stack, svc);
    }

}