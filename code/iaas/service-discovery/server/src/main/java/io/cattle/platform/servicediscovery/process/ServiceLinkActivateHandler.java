package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooq.tools.StringUtils;

@Named
public class ServiceLinkActivateHandler extends AbstractObjectProcessHandler implements ProcessPreListener {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancelink.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceLink link = (InstanceLink)state.getResource();

        if (link.getServiceConsumeMapId() == null) {
            return null;
        }

        ServiceConsumeMap map = objectManager.loadResource(ServiceConsumeMap.class, link.getServiceConsumeMapId());
        String serviceName = map.getName();
        if (StringUtils.isBlank(serviceName)) {
            Service service = objectManager.loadResource(Service.class, map.getConsumedServiceId());
            serviceName = service.getName();
        }

        Instance instance = consumeMapDao.findOneInstanceForService(map.getConsumedServiceId());
        List<String> names = consumeMapDao.findInstanceNamesForService(map.getConsumedServiceId());

        return new HandlerResult(INSTANCE_LINK.LINK_NAME, serviceName,
                InstanceLinkConstants.FIELD_INSTANCE_NAMES, names,
                INSTANCE_LINK.TARGET_INSTANCE_ID, instance == null ? null : instance.getId());
    }

}
