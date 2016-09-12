package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class StackCreatePostListener extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { "stack.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack stack = (Stack) state.getResource();
        List<? extends Object> volumes = DataAccessor.fields(stack)
                .withKey(ServiceDiscoveryConstants.FIELD_VOLUME_TEMPLATES).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, Object.class);
        for (Object volume : volumes) {
            Map<String, Object> props = CollectionUtils.toMap(volume);
            props.put("accountId", stack.getAccountId());
            props.put(ServiceDiscoveryConstants.FIELD_STACK_ID, stack.getId());
            VolumeTemplate template = objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                    stack.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, props.get("name"),
                    VOLUME_TEMPLATE.STACK_ID, stack.getId());
            if (template == null) {
                template = objectManager.create(VolumeTemplate.class, props);
            }

            if (template.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, template, null);
            }
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
