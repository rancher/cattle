package io.cattle.platform.trigger;

import io.cattle.platform.core.addon.metadata.ServiceInfo;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;
import io.cattle.platform.loop.factory.LoopFactoryImpl;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

public class ServiceReconcileTrigger implements Trigger {

    LoopManager loopManager;
    ObjectManager objectManager;

    public ServiceReconcileTrigger(LoopManager loopManager, ObjectManager objectManager) {
        super();
        this.loopManager = loopManager;
        this.objectManager = objectManager;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        for (Long id : getServices(resource)) {
            loopManager.kick(LoopFactoryImpl.RECONCILE, ServiceConstants.KIND_SERVICE, id, resource);
        }
    }

    private Collection<Long> getServices(Object obj) {
        Host host = null;
        if (obj instanceof Host) {
            host = (Host) obj;
        }

        if (host == null && (obj instanceof Agent)) {
            host = objectManager.findAny(Host.class,
                    HOST.AGENT_ID, ((Agent) obj).getId(),
                    HOST.REMOVED, null);
        }

        if (host != null) {
            List<? extends Service> allServices = objectManager.find(Service.class,
                    SERVICE.CLUSTER_ID, host.getClusterId(),
                    SERVICE.REMOVED, null);
            List<Long> svcsToReconcile = new ArrayList<>();
            for (Service service : allServices) {
                if (!ServiceUtil.isActiveService(service)) {
                    continue;
                }
                if (ServiceUtil.isGlobalService(service)) {
                    svcsToReconcile.add(service.getId());
                }
            }
            return svcsToReconcile;
        }

        if (obj instanceof DeploymentUnit) {
            DeploymentUnit du = (DeploymentUnit)obj;
            return Collections.singletonList(du.getServiceId());
        }

        if (obj instanceof GenericObject) {
            Long id = DataAccessor.fieldLong(obj, "serviceId");
            if (id != null) {
                return Collections.singletonList(id);
            }
        }

        if (obj instanceof Instance) {
            Instance instance = (Instance) obj;
            return Collections.singletonList(instance.getServiceId());
        }

        if (obj instanceof Service) {
            return Collections.singletonList(((Service) obj).getId());
        }

        if (obj instanceof ServiceInfo) {
            return Collections.singletonList(((ServiceInfo) obj).getId());
        }

        return Collections.emptyList();
    }

}