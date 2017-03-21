package io.cattle.platform.iaas.api.manager;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostTemplateManager extends AbstractJooqResourceManager {

    private static final Logger log = LoggerFactory.getLogger(HostTemplateManager.class);

    @Inject
    SecretsService secretsService;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getTypes() {
        return new String[] {};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { HostTemplate.class };
    }

    @Override
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
        Object value = DataAccessor.fromMap(properties).withKey("secretValues").get();
        if (value != null) {
            try {
                String encoded = Base64.encodeBase64String(jsonMapper.writeValueAsString(value).getBytes("UTF-8"));
                String newValue = secretsService.encrypt(ApiUtils.getPolicy().getAccountId(), encoded);
                Map<String, Object> empty = emptyMap(CollectionUtils.toMap(value));
                properties.put("secretValues", newValue);
                properties.put("secretValuesEmpty", empty);
            } catch (IOException e) {
                log.error("Failed to encrypt", e);
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE);
            }
        }
        T result = super.createAndScheduleObject(clz, properties);
        DataAccessor.setField(result, "secretValues", value);
        return result;
    }

    protected Map<String, Object> emptyMap(Map<String, Object> obj) {
        Map<String, Object> empty = new HashMap<>();
        obj.forEach((k, v)->{
            if (v instanceof Map<?, ?>) {
                v = emptyMap(CollectionUtils.toMap(v));
            } else {
                v = null;
            }
            empty.put(k, v);
        });
        return empty;
    }

}
