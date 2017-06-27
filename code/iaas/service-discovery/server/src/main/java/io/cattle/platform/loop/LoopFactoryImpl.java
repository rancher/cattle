package io.cattle.platform.loop;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.inator.Deployinator;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;

import org.apache.commons.lang3.StringUtils;

public class LoopFactoryImpl implements LoopFactory {

    public static final String RECONCILE = "service-reconcile";
    public static final String DU_RECONCILE = "deployment-unit-reconcile";
    public static final String STACK_HEALTH = "stack-health";

    Deployinator deployinator;
    ServiceLifecycleManager sdService;
    ObjectManager objectManager;
    ActivityService activityService;
    ObjectProcessManager processManager;

    public LoopFactoryImpl(Deployinator deployinator, ServiceLifecycleManager sdService, ObjectManager objectManager, ActivityService activityService,
            ObjectProcessManager processManager) {
        super();
        this.deployinator = deployinator;
        this.sdService = sdService;
        this.objectManager = objectManager;
        this.activityService = activityService;
        this.processManager = processManager;
    }

    @Override
    public Loop buildLoop(String name, String type, Long id) {
        if (StringUtils.isBlank(name) || StringUtils.isBlank(type) || id == null) {
            return null;
        }

        switch (name) {
        case RECONCILE:
            Service service = objectManager.loadResource(Service.class, id);
            if (service == null) {
                return null;
            }
            return new ReconcileLoop(objectManager, processManager, deployinator, activityService, Service.class, id,
                    id, null, service.getAccountId(), ServiceConstants.KIND_SERVICE);
        case DU_RECONCILE:
            DeploymentUnit unit = objectManager.loadResource(DeploymentUnit.class, id);
            if (unit == null) {
                return null;
            }
            return new ReconcileLoop(objectManager, processManager, deployinator, activityService, DeploymentUnit.class, id,
                    unit.getServiceId(), id, unit.getAccountId(), ServiceConstants.KIND_DEPLOYMENT_UNIT);
        case STACK_HEALTH:
            return new StackHealthLoop(sdService, id);
        default:
            break;
        }

        return null;
    }

}
