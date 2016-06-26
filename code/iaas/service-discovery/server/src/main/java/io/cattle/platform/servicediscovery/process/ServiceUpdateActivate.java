package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.iaas.api.auditing.AuditService;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.deployment.DeploymentManager;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    IdFormatter idFormatter;

    @Inject
    AuditService auditSvc;

    @Inject
    ServiceExposeMapDao exposeDao;

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

        String error = "";
        try {
            waitForConsumedServicesActivate(state);
            if (StringUtils.isEmpty(DataAccessor
                    .fieldString(service, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD))) {
                objectManager.setFields(objectManager.reload(service),
                        ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD,
                        "Reconciling");
            }
            deploymentMgr.activate(service);
        } catch (TimeoutException ex) {
            error = obfuscateId(ex);
            throw ex;
        } catch (FailedToAcquireLockException ex) {
            throw ex;
        } catch (Exception ex) {
            error = ex.getMessage();
            throw ex;
        } finally {
            if (!StringUtils.isEmpty(error)) {
                objectManager.setFields(objectManager.reload(service),
                        ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD,
                        error);
                Map<String, Object> data = new HashMap<>();
                data.put("description", error);
                auditSvc.logResourceModification(service, data, AuditEventType.reconcile, error,
                        service.getAccountId(),
                        null);
            }
        }
        objectManager.reload(state.getResource());
        return new HandlerResult(ServiceDiscoveryConstants.FIELD_CURRENT_SCALE, exposeDao.getCurrentScale(service.getId()));
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
            Object obfuscatedId = null;
            
            if (Instance.class.getSimpleName().equalsIgnoreCase(resourceType)) {
                // in most cases it's going to be instance
                Instance instance = objectManager.loadResource(Instance.class, resourceId);
                if (instance != null && instance.getName() != null) {
                    obfuscatedId = instance.getName();
                }
            }
            
            if (obfuscatedId == null) {
                obfuscatedId = idFormatter.formatId(resourceType, resourceId);
            }

            // append predicated resource's transitioning message to an error
            Object predicateResource = objectManager.loadResource(resourceType, resourceId);
            error = "Waiting for [" + resourceType + ":" + obfuscatedId + "]";
            if (predicateResource != null) {
                String transitioningMsg = DataAccessor.fieldString(predicateResource,
                        "transitioningMessage");
                // Upper case first letter in resourceType
                StringBuffer sb = new StringBuffer(resourceType);
                sb.replace(0, 1, resourceType.substring(0, 1).toUpperCase());

                if (!StringUtils.isEmpty(sb)) {
                    error = error + ". " + sb + " status: " + transitioningMsg;
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
