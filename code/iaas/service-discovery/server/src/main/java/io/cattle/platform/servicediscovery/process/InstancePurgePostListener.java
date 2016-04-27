package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.allocator.service.HostInfo;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.cattle.platform.util.type.Priority;

import java.util.List;

import javax.inject.Inject;

public class InstancePurgePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    @Inject
    ServiceDao serviceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_PURGE };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        List<InstanceHostMap> instanceHostMappings = objectManager.find(InstanceHostMap.class,
                INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId());

        if (instanceHostMappings.size() == 0) {
            return null;
        }
        InstanceHostMap mapping = instanceHostMappings.get(0);
        CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
        HostInfo hostInfo = cm.getHostInfo(mapping.getHostId());

        hostInfo.removeInstance(mapping.getInstanceId());;
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}

