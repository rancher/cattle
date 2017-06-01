package io.cattle.platform.iaas.api.host;

import static io.cattle.platform.core.constants.MachineConstants.CONFIG_FIELD_SUFFIX;
import static io.github.ibuildthecloud.gdapi.util.ResponseCodes.UNPROCESSABLE_ENTITY;

import io.cattle.platform.core.model.HostTemplate;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.Map;

public class HostTemplateValidationFilter extends AbstractDefaultResourceManagerFilter {

    static final String DRIVER_CONFIG_EXACTLY_ONE_REQUIRED = "DriverConfigExactlyOneRequired";
    static final String DRIVER_CONFIG_FOR_WRONG_DRIVER = "DriverConfigForWrongDriver";
    static final String STRING_FIELD_EMPTY = "StringFieldEmpty";
    static final String STRING_FIELD_ILL_FORMED = "StringFieldIllFormed";

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{HostTemplate.class};
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<String, Object> obj = CollectionUtils.toMap(request.getRequestObject());

        String driver = validateStringField(obj, "driver");
        String name = validateStringField(obj, "name");
        validateConfigKey(obj, driver);

        return super.create(type, request, next);
    }

    String validateConfigKey(Map<String, Object> obj, String driver) {
        String foundConfigKey = null;
        String[] holderKeys = {"secretValues", "publicValues"};
        for (String holderKey : holderKeys) {
            Map<String, Map<String, Object>> configs = CollectionUtils.toMap(obj.get(holderKey));
            for (Map.Entry<String, Map<String, Object>> configEntry : configs.entrySet()) {
                if (configEntry.getKey().endsWith(CONFIG_FIELD_SUFFIX) && configEntry.getValue() != null) {
                    if (foundConfigKey != null && !foundConfigKey.equals(configEntry.getKey())) {
                        throw new ClientVisibleException(UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
                    }
                    foundConfigKey = configEntry.getKey();
                }
            }
        }
        if (foundConfigKey == null) {
            throw new ClientVisibleException(UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
        }
        if (!(driver + CONFIG_FIELD_SUFFIX).equals(foundConfigKey)) {
            throw new ClientVisibleException(UNPROCESSABLE_ENTITY, DRIVER_CONFIG_FOR_WRONG_DRIVER);
        }
        return foundConfigKey;
    }

    String validateStringField(Map<String, Object> obj, String fieldName) {
        String s = (String) obj.get(fieldName);
        if (!s.equals(s.trim())) {
            throw new ClientVisibleException(UNPROCESSABLE_ENTITY, STRING_FIELD_ILL_FORMED, null, fieldName);
        }
        if (s.length() == 0) {
            throw new ClientVisibleException(UNPROCESSABLE_ENTITY, STRING_FIELD_EMPTY, null, fieldName);
        }
        return s;
    }

}
