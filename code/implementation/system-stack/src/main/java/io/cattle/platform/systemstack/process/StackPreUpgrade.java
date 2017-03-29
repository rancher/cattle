package io.cattle.platform.systemstack.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class StackPreUpgrade extends AbstractObjectProcessHandler implements ProcessPreListener {

    @Inject
    StackPreCreate preCreate;

    @Inject
    CatalogService catalogService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (!catalogService.isEnabled()) {
            return null;
        }

        Stack stack = (Stack)state.getResource();

        String externalId = DataAccessor
                .fromMap(state.getData())
                .withKey(ServiceConstants.STACK_FIELD_EXTERNAL_ID)
                .as(String.class);
        String compose = DataAccessor
                .fromMap(state.getData())
                .withKey(ServiceConstants.STACK_FIELD_DOCKER_COMPOSE)
                .as(String.class);
        String rancherCompose = DataAccessor
                .fromMap(state.getData())
                .withKey(ServiceConstants.STACK_FIELD_RANCHER_COMPOSE)
                .as(String.class);
        Map<String, Object> templates = CollectionUtils.toMap(DataAccessor
                .fromMap(state.getData())
                .withKey(ServiceConstants.STACK_FIELD_TEMPLATES)
                .getForWrite());


        if (StringUtils.isBlank(externalId)) {
            return null;
        }

        if (StringUtils.isBlank(compose) &&
            StringUtils.isBlank(rancherCompose) &&
            templates.isEmpty()) {
            Map<String, Object> data = preCreate.externalIdToData(stack, externalId);
            return new HandlerResult(data);
        }

        return null;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_UPGRADE };
    }

}
