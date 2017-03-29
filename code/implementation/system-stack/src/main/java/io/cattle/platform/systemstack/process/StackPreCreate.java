package io.cattle.platform.systemstack.process;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.systemstack.catalog.CatalogService;
import io.cattle.platform.systemstack.model.Template;
import io.cattle.platform.util.type.CollectionUtils;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class StackPreCreate extends AbstractObjectProcessHandler implements ProcessPreListener {

    @Inject
    CatalogService catalogService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if (!catalogService.isEnabled()) {
            return null;
        }

        Stack stack = (Stack)state.getResource();
        String externalId = stack.getExternalId();

        if (StringUtils.isBlank(externalId)) {
            return null;
        }

        if (StringUtils.isBlank(DataAccessor.fieldString(stack, ServiceConstants.STACK_FIELD_DOCKER_COMPOSE)) &&
            StringUtils.isBlank(DataAccessor.fieldString(stack, ServiceConstants.STACK_FIELD_RANCHER_COMPOSE)) &&
            DataAccessor.fieldMap(stack, ServiceConstants.STACK_FIELD_TEMPLATES).isEmpty()) {
            Map<String, Object> data = externalIdToData(stack, externalId);
            return new HandlerResult(data);
        }

        return null;
    }

    public Map<String, Object> externalIdToData(Stack stack, String externalId) {
        String name = stack.getName();
        Template template = null;
        String namespace = null;

        try {
            template = catalogService.lookupTemplate(externalId);

            if (template == null) {
                return null;
            }

            namespace = DataAccessor.fieldString(stack, "namespace");
            if (StringUtils.isBlank(namespace)) {
                namespace = name;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to lookup catalog template", e);
        }

        return CollectionUtils.asMap(
                ObjectMetaDataManager.NAME_FIELD, name,
                "namespace", namespace,
                ServiceConstants.STACK_FIELD_DOCKER_COMPOSE, template.getDockerCompose(),
                ServiceConstants.STACK_FIELD_RANCHER_COMPOSE, template.getRancherCompose(),
                ServiceConstants.STACK_FIELD_TEMPLATES, template.getFiles(),
                ServiceConstants.STACK_FIELD_EXTERNAL_ID, template.getExternalId());
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceConstants.PROCESS_STACK_CREATE };
    }

}
