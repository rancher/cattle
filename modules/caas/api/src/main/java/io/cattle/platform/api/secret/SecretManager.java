package io.cattle.platform.api.secret;

import io.cattle.platform.api.resource.DefaultResourceManager;
import io.cattle.platform.api.resource.DefaultResourceManagerSupport;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class SecretManager extends DefaultResourceManager {

    private static final Logger log = LoggerFactory.getLogger(SecretManager.class);

    SecretsService secretsService;

    public SecretManager(DefaultResourceManagerSupport support, SecretsService secretsService) {
        super(support);
        this.secretsService = secretsService;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Map<String, Object> properties = CollectionUtils.toMap(request.getRequestObject());
        String value = DataAccessor.fromMap(properties).withKey("value").as(String.class);
        if (StringUtils.isNotBlank(value)) {
            try {
                String newValue = secretsService.encrypt(ApiUtils.getPolicy().getAccountId(), value);
                properties.put("value", newValue);
                request.setRequestObject(properties);
            } catch (IOException e) {
                log.error("Failed to secret", e);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE);
            }
        }
        Object result = super.create(type, request);
        if (result instanceof Secret) {
            ((Secret) result).setValue(value);
        }
        return result;
    }


}
