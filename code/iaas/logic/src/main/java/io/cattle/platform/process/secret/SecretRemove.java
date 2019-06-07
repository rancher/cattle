package io.cattle.platform.process.secret;

import io.cattle.platform.core.model.Secret;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SecretRemove extends AbstractDefaultProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(SecretRemove.class);

    @Inject
    SecretsService secretsService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Secret secret = (Secret) state.getResource();
        String secretValue = secret.getValue();
        if (StringUtils.isNotBlank(secretValue)) {
            try {
                secretsService.delete(secret.getAccountId(), secret.getValue());
            } catch (IOException e) {
                log.error("Failed to delete secret from storage [{}]", secret.getId(), e);
                throw new IllegalStateException(e);
            }
        }
        return null;
    }

}