package io.cattle.platform.process.hosttemplate;

import io.cattle.platform.core.constants.HostTemplateConstants;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HosttemplateRemove implements ProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(HosttemplateRemove.class);

    SecretsService secretsService;

    public HosttemplateRemove(SecretsService secretsService) {
        this.secretsService = secretsService;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HostTemplate template = (HostTemplate)state.getResource();
        String value = DataAccessor.fieldString(template, HostTemplateConstants.FIELD_SECRET_VALUES);
        try {
            secretsService.delete(value);
        } catch (IOException e) {
            log.error("Failed to delete secret from storage for machine driver credential [{}]",
                    template.getId(), e);
        }
        return null;
    }

}
