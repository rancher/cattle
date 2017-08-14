package io.cattle.platform.process.secret;

import io.cattle.platform.core.model.Secret;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.framework.secret.SecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SecretRemove implements ProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(SecretRemove.class);

    SecretsService secretsService;

    public SecretRemove(SecretsService secretsService) {
        this.secretsService = secretsService;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Secret secret = (Secret)state.getResource();
        try {
            secretsService.delete(secret.getValue());
        } catch (IOException e) {
            log.error("Failed to delete secret from storage [{}]",
                    secret.getId(), e);
            throw new IllegalStateException(e);
        }
        return null;
    }

}