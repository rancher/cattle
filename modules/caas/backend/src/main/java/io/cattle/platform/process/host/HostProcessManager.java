package io.cattle.platform.process.host;

import io.cattle.platform.async.utils.ResourceTimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.docker.constants.DockerHostConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class HostProcessManager {

    InstanceDao instanceDao;
    ResourceMonitor resourceMonitor;
    EventService eventService;
    HostDao hostDao;
    ObjectMetaDataManager objectMetaDataManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public HostProcessManager(InstanceDao instanceDao, ResourceMonitor resourceMonitor, EventService eventService, HostDao hostDao,
            ObjectMetaDataManager objectMetaDataManager, ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.instanceDao = instanceDao;
        this.resourceMonitor = resourceMonitor;
        this.eventService = eventService;
        this.hostDao = hostDao;
        this.objectMetaDataManager = objectMetaDataManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Host host = (Host)state.getResource();
        String driver = getDriver(host);
        if (StringUtils.isBlank(driver)) {
            driver = getDriverFromHostTemplate(host);
        }

        if (StringUtils.isBlank(driver)) {
            return new HandlerResult().withChainProcessName(HostConstants.PROCESS_ACTIVATE);
        }

        PhysicalHost phyHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (phyHost == null) {
            phyHost = hostDao.createMachineForHost(host, driver);
        }

        return new HandlerResult(MachineConstants.FIELD_DRIVER, driver).withChainProcessName(HostConstants.PROCESS_PROVISION);
    }

    public static String getDriver(Object obj) {
        Map<String, Object> fields = DataUtils.getFields(obj);
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (StringUtils.endsWithIgnoreCase(field.getKey(), MachineConstants.CONFIG_FIELD_SUFFIX) && field.getValue() != null) {
                return StringUtils.removeEndIgnoreCase(field.getKey(), MachineConstants.CONFIG_FIELD_SUFFIX);
            }
        }

        return null;
    }

    private String getDriverFromHostTemplate(Host host) {
        Long hostTemplateId = host.getHostTemplateId();
        if (hostTemplateId != null) {
            HostTemplate ht = objectManager.loadResource(HostTemplate.class, hostTemplateId);
            return ht.getDriver();
        }

        return null;
    }

    public HandlerResult provision(ProcessState state, ProcessInstance process) {
        final Host host = (Host)state.getResource();
        PhysicalHost physicalHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (physicalHost == null) {
            return null;
        }

        physicalHost = resourceMonitor.waitFor(physicalHost, 5 * 60000, new ResourcePredicate<PhysicalHost>() {
            @Override
            public boolean evaluate(final PhysicalHost obj) {
                boolean transitioning = objectMetaDataManager.isTransitioningState(obj.getClass(),
                        obj.getState());
                if (!transitioning) {
                    return true;
                }

                objectManager.reload(host);
                if (!host.getState().equals(HostConstants.STATE_PROVISIONING)) {
                    throw new ResourceTimeoutException(host, "provisioning canceled");
                }

                String message = TransitioningUtils.getTransitioningMessage(obj);
                objectManager.setFields(host, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, message);
                DeferredUtils.nest(new Runnable() {
                    @Override
                    public void run() {
                        ObjectUtils.publishChanged(eventService, objectManager, host);
                    }
                });

                return false;
            }

            @Override
            public String getMessage() {
                return "machine to provision";
            }
        });

        if (!CommonStatesConstants.ACTIVE.equals(physicalHost.getState())) {
            processManager.scheduleStandardProcess(StandardProcess.ERROR, host, null);
            String message = TransitioningUtils.getTransitioningMessage(physicalHost);
            ExecutionException e = new ExecutionException(message);
            e.setResources(host);
            throw e;
        }

        try {
            resourceMonitor.waitFor(host, 5 * 60000, new ResourcePredicate<Host>() {
                @Override
                public boolean evaluate(Host obj) {
                    if (!host.getState().equals(HostConstants.STATE_PROVISIONING)) {
                        throw new ResourceTimeoutException(host, "provisioning canceled");
                    }

                    return obj.getAgentId() != null;
                }

                @Override
                public String getMessage() {
                    return "agent to check in";
                }
            });
        } catch (ResourceTimeoutException rte) {
            processManager.scheduleStandardProcess(StandardProcess.ERROR, host, null);
            ExecutionException e = new ExecutionException(rte.getMessage());
            e.setResources(host);
            throw e;
        }

        return new HandlerResult(
                MachineConstants.FIELD_DRIVER, physicalHost.getDriver()
                ).withChainProcessName(processManager.getProcessName(host, StandardProcess.ACTIVATE));
    }

    public HandlerResult remove(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        removeInstances(host);
        removePools(host);

        PhysicalHost physHost = objectManager.loadResource(PhysicalHost.class, host.getPhysicalHostId());
        if (physHost != null) {
            processManager.executeDeactivateThenScheduleRemove(physHost, null);
        }

        return null;
    }

    protected void removePools(Host host) {
        for (StoragePoolHostMap map : objectManager.children(host, StoragePoolHostMap.class)) {
            StoragePool pool = objectManager.loadResource(StoragePool.class, map.getStoragePoolId());
            if (DockerHostConstants.KIND_DOCKER.equals(pool.getKind())) {
                processManager.executeDeactivateThenRemove(pool, null);
            }
            processManager.executeDeactivateThenRemove(map, null);
        }
    }

    protected void removeInstances(Host host) {
        for (Instance instance : instanceDao.getNonRemovedInstanceOn(host.getId())) {
            processManager.stopThenRemove(instance, null);
        }
    }

}
