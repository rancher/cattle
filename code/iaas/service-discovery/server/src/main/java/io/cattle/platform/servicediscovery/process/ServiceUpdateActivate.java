package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.activity.ActivityService;
import io.cattle.platform.core.addon.InServiceUpgradeStrategy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.iaas.api.auditing.AuditService;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;

/**
 * This handler is responsible for activating the service as well as restoring the active service to its scale
 * The handler can be invoked as a part of service.activate, service.update for both scaleUp and ScaleDown
 *
 */
@Named
public class ServiceUpdateActivate extends AbstractObjectProcessHandler {

    @Inject
    ActivityService activity;
    @Inject
    DeploymentManager deploymentMgr;
    @Inject
    ServiceDiscoveryService serviceDiscoveryService;
    @Inject
    ResourceMonitor resourceMonitor;
    @Inject
    IdFormatter idFormatter;
    @Inject
    AuditService auditSvc;
    @Inject
    ServiceExposeMapDao exposeDao;
    @Inject
    UpgradeManager upgradeMgr;
    @Inject
    ServiceDao serviceDao;
    @Inject
    ServiceDiscoveryService sdSvc;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, final ProcessInstance process) {
        final Service service = (Service) state.getResource();
        // on inactive service update, do nothing
        if (process.getName().equalsIgnoreCase(ServiceConstants.PROCESS_SERVICE_UPDATE)
                && service.getState().equalsIgnoreCase(CommonStatesConstants.UPDATING_INACTIVE)) {
            return null;
        }
        activity.run(service, process.getName(), getMessage(process.getName()), new Runnable() {
            @Override
            public void run() {
                // v2 upgrade
                if (ServiceConstants.SERVICE_LIKE.contains(service.getKind())
                        && service.getIsUpgrade()) {
                    Pair<InstanceRevision, InstanceRevision> currentPreviousRevision = serviceDao
                            .getCurrentAndPreviousRevisions(service);
                    InServiceUpgradeStrategy strategy = ServiceDiscoveryUtil.getStrategy(service,
                            currentPreviousRevision, true);
                    if (service.getState().equalsIgnoreCase(CommonStatesConstants.UPDATING_ACTIVE)) {
                        boolean prePullImage = DataAccessor.fieldBool(service, ServiceConstants.FIELD_IMAGE_PRE_PULL);
                        upgradeMgr.upgrade(service, strategy, service.getState(), true, prePullImage);
                    } else {
                        upgradeMgr.upgrade(service, strategy, service.getState(), false, false);
                    }
                    sdSvc.resetUpgradeFlag(service);
                }
                deploymentMgr.activate(service);
            }
        });

        objectManager.reload(state.getResource());
        return new HandlerResult(ServiceConstants.FIELD_CURRENT_SCALE, exposeDao.getCurrentScale(service.getId()));
    }


    protected String getMessage(String name) {
        if (name == null) {
            return null;
        }
        switch (name) {
        case "service.activate":
            return "Activating service";
        default:
            return "Updating service";
        }
    }
}
