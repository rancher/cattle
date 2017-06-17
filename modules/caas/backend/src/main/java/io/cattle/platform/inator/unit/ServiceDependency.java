package io.cattle.platform.inator.unit;

import io.cattle.platform.core.addon.DependsOn;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.deploy.DeploymentUnitInator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.wrapper.ServiceWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.Collection;
import java.util.Collections;

public class ServiceDependency implements Unit {

    DependsOn dependsOn;
    InatorServices svc;

    public ServiceDependency(DependsOn dependsOn, InatorServices svc) {
        this.dependsOn = dependsOn;
        this.svc = svc;
    }

    @Override
    public Result scheduleActions(InatorContext context) {
        StackWrapper stack = getStack(context);
        if (stack == null) {
            return Result.good();
        }

        String[] parts = dependsOn.getService().split("/");
        String serviceName = parts[0];
        String stackName = stack.getName();
        if (parts.length > 1) {
            stackName = parts[0];
            serviceName = parts[1];
        }

        Service service = svc.serviceDao.findServiceByName(stack.getAccountId(), serviceName, stackName);
        ServiceWrapper serviceWrapper = service == null ? null : new ServiceWrapper(service, svc);
        InstanceUnit localDep = null;
        if (service == null && stackName.equals(stack.getName())) {
            for (Unit unit : context.getUnits().values()) {
                if (unit instanceof InstanceUnit && serviceName.equals(((InstanceUnit) unit).getServiceName())) {
                    localDep = (InstanceUnit) unit;
                }
            }
        }

        if (serviceWrapper != null) {
            if ((dependsOn.getCondition() == DependsOn.DependsOnCondition.running && serviceWrapper.isActive()) ||
                (dependsOn.getCondition() == DependsOn.DependsOnCondition.healthy && serviceWrapper.isActive() && serviceWrapper.isHealthy())) {
                return Result.good();
            }
        } else if (localDep != null) {
            if ((dependsOn.getCondition() == DependsOn.DependsOnCondition.running && localDep.getWrapper().isActive()) ||
                (dependsOn.getCondition() == DependsOn.DependsOnCondition.healthy && localDep.getWrapper().isActive())) {
                return Result.good();
            }
        }

        return new Result(UnitState.WAITING, this, String.format("%s/%s to be %s", stackName, serviceName,
                dependsOn.getCondition()));
    }

    private StackWrapper getStack(InatorContext context) {
        Inator inator = context.getInator();
        if (inator instanceof DeploymentUnitInator) {
            return ((DeploymentUnitInator) inator).getStack();
        }
        return null;
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        return Result.good();
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return Collections.emptyList();
    }

    @Override
    public UnitRef getRef() {
        return new UnitRef(String.format("dependency/%s/%s", dependsOn.getService(), dependsOn.getCondition()));
    }

    @Override
    public Result remove(InatorContext context) {
        return Result.good();
    }

    @Override
    public String getDisplayName() {
        return String.format("%s %s", dependsOn.getService(), dependsOn.getCondition());
    }

}
