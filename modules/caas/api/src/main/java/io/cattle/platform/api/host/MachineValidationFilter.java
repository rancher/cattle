package io.cattle.platform.api.host;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static io.cattle.platform.core.constants.HostConstants.*;

public class MachineValidationFilter extends AbstractValidationFilter {

    private static final String DRIVER_CONFIG_EXACTLY_ONE_REQUIRED = "DriverConfigExactlyOneRequired";
    private static final Logger log = LoggerFactory.getLogger(MachineValidationFilter.class);

    SecretsService secretsService;

    public MachineValidationFilter(SecretsService secretsService) {
        super();
        this.secretsService = secretsService;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        // Don't validate hosts for v1 API
        if (HostConstants.TYPE.equals(type) && ("v1".equals(request.getVersion()) ||
                AccountConstants.SUPER_ADMIN_KIND.equals(request.getSchemaFactory().getId()))) {
            return super.create(type, request, next);
        }

        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        boolean alreadyFound = false;

        for (Map.Entry<String, Object> field : data.entrySet()) {
            if (StringUtils.endsWithIgnoreCase(field.getKey(), CONFIG_FIELD_SUFFIX) && field.getValue() != null) {
                if (alreadyFound) {
                    throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
                }
                alreadyFound = true;
            }
        }

        if (!alreadyFound && data.get(HostConstants.FIELD_HOST_TEMPLATE_ID) == null) {
            throw new ClientVisibleException(ResponseCodes.UNPROCESSABLE_ENTITY, DRIVER_CONFIG_EXACTLY_ONE_REQUIRED);
        }
        return super.create(type, request, next);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
        String extracted = DataAccessor.fromMap(data).withKey(EXTRACTED_CONFIG_FIELD).as(String.class);
        if (extracted != null) {
            try {
                extracted = secretsService.encrypt(ApiUtils.getPolicy().getAccountId(), extracted);
            } catch (IOException e) {
                log.error("Failed to encrypt machine secrets", e);
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "FailedEncryption");
            }
            data.put(EXTRACTED_CONFIG_FIELD, extracted);
        }
        return super.update(type, id, request, next);
    }

}
