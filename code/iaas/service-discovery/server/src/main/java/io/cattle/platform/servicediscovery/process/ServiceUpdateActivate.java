package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

/**
 * This handler is responsible for activating the service as well as restoring the active service to its scale
 * The handler can be invoked as a part of service.activate, service.update for both scaleUp and ScaleDown
 *
 */
@Named
public class ServiceUpdateActivate extends AbstractObjectProcessHandler {

    @Inject
    DeploymentManager deploymentMgr;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    ProcessProgress progress;

    @Inject
    IdFormatter idFormatter;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();

        // on inactive service update, do nothing
        if (process.getName().equalsIgnoreCase(ServiceDiscoveryConstants.PROCESS_SERVICE_UPDATE)
                && service.getState().equalsIgnoreCase(CommonStatesConstants.UPDATING_INACTIVE)) {
            return null;
        }

        progress.init(state, 50, 50);

        String error = "";
        try {
            progress.checkPoint("Activating consumed services");
            waitForConsumedServicesActivate(state);
            progress.checkPoint("Reconciling");
            deploymentMgr.activate(service);
        } catch (TimeoutException ex) {
            error = obfuscateId(ex);
            throw ex;
        } catch (Exception ex) {
            error = ex.getMessage();
            throw ex;
        } finally {
            if (!StringUtils.isEmpty(error)) {
                objectManager.setFields(objectManager.reload(service),
                        ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD,
                        error);
            }
        }

        return null;
    }

    protected String obfuscateId(TimeoutException ex) {
        String error = ex.getMessage();
        if (ex.getMessage().contains(ResourceMonitor.ERROR_MSG)) {
            // obfuscate id of predicated resource
            String[] msg = ex.getMessage().split("\\]");
            String[] splittedForId = msg[0].split(":");
            String[] splittedForResourceType = splittedForId[0].split("\\[");
            String resourceId = splittedForId[1];
            String resourceType = splittedForResourceType[1];
            Object obfuscatedId = idFormatter.formatId(resourceType, resourceId);
            error = error.replace(resourceId + "]", obfuscatedId + "]");

            // append predicated resource's transitioninig message to an error
            Object predicateResource = objectManager.loadResource(resourceType, resourceId);
            if (predicateResource != null) {
                String transitioningMsg = DataAccessor.fieldString(predicateResource,
                        "transitioningMessage");
                if (!StringUtils.isEmpty(transitioningMsg)) {
                    error = error + ". " + resourceType + " status: " + transitioningMsg;
                }
            }
        }

        return error;
    }

    @SuppressWarnings("unchecked")
    protected void waitForConsumedServicesActivate(ProcessState state) {
        List<Integer> consumedServicesIds = DataAccessor.fromMap(state.getData())
                .withKey(ServiceDiscoveryConstants.FIELD_WAIT_FOR_CONSUMED_SERVICES_IDS)
                .withDefault(Collections.EMPTY_LIST).as(List.class);

        for (Integer consumedServiceId : consumedServicesIds) {
            Service consumedService = objectManager.loadResource(Service.class, consumedServiceId.longValue());
            resourceMonitor.waitFor(consumedService,
                    new ResourcePredicate<Service>() {
                        @Override
                        public boolean evaluate(Service obj) {
                            return CommonStatesConstants.ACTIVE.equals(obj.getState());
                        }
                    });
        }
    }
}
