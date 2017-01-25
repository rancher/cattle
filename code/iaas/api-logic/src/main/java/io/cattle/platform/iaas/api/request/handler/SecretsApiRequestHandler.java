package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.SecretDao;
import io.cattle.platform.core.dao.SecretDao.InstanceAndHost;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.framework.secret.SecretValue;
import io.cattle.platform.framework.secret.SecretsService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractResponseGenerator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class SecretsApiRequestHandler extends AbstractResponseGenerator {

    private String CONTENT_TYPE = "application/x-api-secrets-token";

    @Inject
    ObjectManager objectManager;
    @Inject
    TokenService tokenService;
    @Inject
    SecretDao secretDao;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    SecretsService secretsService;

    @Override
    protected void generate(final ApiRequest request) throws IOException {
        if (!"secret".equals(request.getType()) || !"POST".equals(request.getMethod())) {
            return;
        }

        if (!CONTENT_TYPE.equalsIgnoreCase(request.getServletContext().getRequest().getContentType())) {
            return;
        }

        String token = request.proxyRequestObject(Secret.class).getValue();
        Map<String, Object> value = null;
        try {
            value = tokenService.getJsonPayload(token, false);
        } catch (TokenException e) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }

        String uuid = DataAccessor.fromMap(value).withKey("uuid").as(String.class);
        if (StringUtils.isBlank(uuid)) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }

        InstanceAndHost ih = secretDao.getHostForInstanceUUIDAndAuthAccount(ApiUtils.getPolicy().getAccountId(), uuid);
        if (ih == null) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }

        List<SecretReference> secrets = DataAccessor.fieldObjectList(ih.instance, InstanceConstants.FIELD_SECRETS, SecretReference.class, jsonMapper);
        List<SecretValue> values = secretsService.getValues(secrets, ih.host);

        jsonMapper.writeValue(request.getOutputStream(), values);
        request.setResponseObject(new Object());
    }

}
