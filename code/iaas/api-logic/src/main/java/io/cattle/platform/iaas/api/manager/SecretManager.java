package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretManager extends AbstractJooqResourceManager {

    private static final Logger log = LoggerFactory.getLogger(SecretManager.class);

    @Inject
    SecretsService secretsService;

    @Override
    public String[] getTypes() {
        return new String[] {};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Secret.class };
    }

    @Override
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
        String value = DataAccessor.fromMap(properties).withKey("value").as(String.class);
        if (StringUtils.isNotBlank(value)) {
            try {
                String newValue = secretsService.encrypt(ApiUtils.getPolicy().getAccountId(), value);
                properties.put("value", newValue);
            } catch (IOException e) {
                log.error("Failed to secret", e);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE);
            }
        }
        T result = super.createAndScheduleObject(clz, properties);
        if (result instanceof Secret) {
            ((Secret) result).setValue(value);
        }
        return result;
    }


}
