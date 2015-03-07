package io.cattle.platform.process.link;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InstanceInstanceLinkCreate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    private static final Logger log = LoggerFactory.getLogger(InstanceInstanceLinkCreate.class);

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        ObjectManager objectManager = getObjectManager();

        Object linkObjs = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_INSTANCE_LINKS).withDefault(new HashMap<String, Object>()).get();

        if (!(linkObjs instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, InstanceLink> links = new HashMap<String, InstanceLink>();
        for (InstanceLink link : objectManager.children(instance, InstanceLink.class, InstanceLinkConstants.FIELD_INSTANCE_ID)) {
            links.put(link.getLinkName(), link);
        }

        for (Map.Entry<?, ?> entry : ((Map<?, ?>) linkObjs).entrySet()) {
            String name = entry.getKey().toString();
            Long targetInstanceId = null;

            try {
                if (entry.getValue() != null) {
                    targetInstanceId = Long.parseLong(entry.getValue().toString());
                }
            } catch (NumberFormatException nfe) {
                log.error("Invalid instance [{}] for link [{}], skipping", entry.getValue(), entry.getKey());
                continue;
            }

            if (links.containsKey(name)) {
                continue;
            }

            InstanceLink linkObj = objectManager.create(InstanceLink.class, INSTANCE_LINK.ACCOUNT_ID, instance.getAccountId(), INSTANCE_LINK.LINK_NAME, name,
                    INSTANCE_LINK.INSTANCE_ID, instance.getId(), INSTANCE_LINK.TARGET_INSTANCE_ID, targetInstanceId);

            links.put(name, linkObj);
        }

        for (InstanceLink link : links.values()) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, link, state.getData());
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
