package io.cattle.platform.process.hosttemplate;

import io.cattle.platform.core.constants.HostTemplateConstants;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class HosttemplateRemove extends AbstractDefaultProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(HosttemplateRemove.class);

    @Inject
    SecretsService secretsService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        HostTemplate template = (HostTemplate)state.getResource();
        String value = DataAccessor.fieldString(template, HostTemplateConstants.FIELD_SECRET_VALUES);
        try {
            secretsService.delete(template.getAccountId(), value);
        } catch (IOException e) {
            log.error("Failed to delete secret from storage for machine driver credential [{}]",
                    template.getId(), e);
        }
        return null;
    }

}
