package io.cattle.platform.process.host;

import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.StoragePoolHostMap;
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
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.resource.UUID;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

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

        String externalId = host.getExternalId();
        if (externalId == null) {
            externalId = UUID.randomUUID().toString();
        }

        String chainProcess = HostConstants.PROCESS_PROVISION;
        if (StringUtils.isBlank(driver)) {
            chainProcess = HostConstants.PROCESS_ACTIVATE;
        }

        return new HandlerResult(
                HostConstants.FIELD_EXTERNAL_ID, externalId,
                HostConstants.FIELD_DRIVER, driver)
                .withChainProcessName(chainProcess);
    }

    public static String getDriver(Object obj) {
        Map<String, Object> fields = DataAccessor.getFields(obj);
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (StringUtils.endsWithIgnoreCase(field.getKey(), HostConstants.CONFIG_FIELD_SUFFIX) && field.getValue() != null) {
                return StringUtils.removeEndIgnoreCase(field.getKey(), HostConstants.CONFIG_FIELD_SUFFIX);
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
        return new HandlerResult()
                .withChainProcessName(processManager.getProcessName(state.getResource(), StandardProcess.ACTIVATE));
    }

    public HandlerResult remove(final ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();

        removeInstances(host);
        removePools(host);

        return null;
    }

    protected void removePools(Host host) {
        for (StoragePoolHostMap map : objectManager.children(host, StoragePoolHostMap.class)) {
            StoragePool pool = objectManager.loadResource(StoragePool.class, map.getStoragePoolId());
            if (DockerHostConstants.KIND_DOCKER.equals(pool.getKind())) {
                processManager.executeDeactivateThenRemove(pool, null);
            }
            objectManager.delete(map);
        }
    }

    protected void removeInstances(Host host) {
        for (Instance instance : instanceDao.getNonRemovedInstanceOn(host.getId())) {
            processManager.stopThenRemove(instance, null);
        }
    }

}
