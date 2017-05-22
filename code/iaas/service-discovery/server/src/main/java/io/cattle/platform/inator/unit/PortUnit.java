package io.cattle.platform.inator.unit;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.InstanceBindable;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.deploy.DeploymentUnitInator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ResourceExhaustionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PortUnit implements Unit, InstanceBindable {

    String name;
    String portNum;
    InatorServices svc;
    UnitRef ref;

    public PortUnit(String name, int portNum, InatorServices svc) {
        this.name = name;
        this.portNum = Integer.toString(portNum);
        this.svc = svc;
        this.ref = new UnitRef("port/" + getSubOwner());
    }

    @Override
    public Result scheduleActions(InatorContext context) {
        return Result.good();
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
        return ref;
    }

    protected DeploymentUnitWrapper getDeploymentUnit(InatorContext context) {
        Inator inator = context.getInator();
        if (inator instanceof DeploymentUnitInator) {
            return ((DeploymentUnitInator) inator).getUnit();
        }
        return null;
    }

    @Override
    public Result remove(InatorContext context) {
        Object owner = getOwner(getDeploymentUnit(context));
        Account account = getAccount(owner);
        if (account == null) {
            return Result.good();
        }
        svc.poolManager.releaseResource(account, owner, new PooledResourceOptions()
                .withSubOwner(getSubOwner())
                .withQualifier(ResourcePoolConstants.ENVIRONMENT_PORT));
        return Result.good();
    }

    protected String getSubOwner() {
        return name + "/" + portNum;
    }

    protected Account getAccount(Object owner) {
        Object accountId = ObjectUtils.getAccountId(owner);
        return svc.objectManager.loadResource(Account.class, accountId == null ? null : accountId.toString());
    }

    protected Object getOwner(DeploymentUnitWrapper unit) {
        Service service = svc.objectManager.loadResource(Service.class, unit.getServiceId());
        if (service != null) {
            return service;
        }
        return unit.getInternal();
    }

    @Override
    public String getDisplayName() {
        return String.format("randomport(%d)", getSubOwner());
    }

    @Override
    public void bind(InatorContext context, Map<String, Object> instanceData) {
        @SuppressWarnings("unchecked")
        List<String> ports = (List<String>)CollectionUtils.toList(instanceData.get(InstanceConstants.FIELD_PORTS));
        boolean changed = false;
        int port = Integer.parseInt(portNum);

        for (int i = 0 ; i < ports.size() ; i++) {
            try {
                PortSpec spec = new PortSpec(ports.get(i));
                if (spec.getPublicPort() == null && spec.getPrivatePort() == port) {
                    Object owner = getOwner(getDeploymentUnit(context));
                    Account account = getAccount(owner);
                    PooledResource resource = svc.poolManager.allocateOneResource(account, owner,
                            new PooledResourceOptions()
                                .withSubOwner(getSubOwner())
                                .withQualifier(ResourcePoolConstants.ENVIRONMENT_PORT));
                    if (resource == null) {
                        throw new ResourceExhaustionException("Not enough environment ports");
                    }
                    spec.setPublicPort(Integer.parseInt(resource.getName()));
                    ports.set(i, spec.toSpec());
                    changed = true;
                    break;
                }
            } catch (ClientVisibleException e) {
            }
        }

        if (changed) {
            instanceData.put(InstanceConstants.FIELD_PORTS, ports);
        }
    }

}
