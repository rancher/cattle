package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;


@Named
public class ServiceLinkPreInstanceStateHandler extends AbstractObjectProcessHandler implements ProcessPreListener {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Override
    public String[] getProcessNames() {
        return new String[]{InstanceConstants.PROCESS_START};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        Set<Long> serviceConsumeMapIds = new HashSet<>();
        Set<InstanceLink> links = new HashSet<>();
        Set<InstanceLink> toRemove = new HashSet<>();

        for (ServiceConsumeMap map : consumeMapDao.findConsumedServicesForInstance(instance.getId(), "service") ) {
            serviceConsumeMapIds.add(map.getId());
        }

        for (InstanceLink link : consumeMapDao.findServiceBasedInstanceLinks(instance.getId())) {
            if (serviceConsumeMapIds.remove(link.getServiceConsumeMapId())) {
                links.add(link);
            } else {
                toRemove.add(link);
            }
        }

        for (InstanceLink remove : toRemove) {
            deactivateThenRemove(remove, state.getData());
        }

        for (Long serviceConsumeMapId : serviceConsumeMapIds) {
            InstanceLink link = objectManager.create(InstanceLink.class,
                    INSTANCE_LINK.ACCOUNT_ID, instance.getAccountId(),
                    INSTANCE_LINK.INSTANCE_ID, instance.getId(),
                    INSTANCE_LINK.SERVICE_CONSUME_MAP_ID, serviceConsumeMapId);
            links.add(link);
        }

        for (InstanceLink link : links) {
            create(link, state.getData());
        }

        return null;
    }
}
