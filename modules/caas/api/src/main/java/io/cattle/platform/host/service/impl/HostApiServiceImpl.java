package io.cattle.platform.host.service.impl;

import static io.cattle.platform.server.context.ServerContext.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.host.api.HostApiUtils;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiRSAKeyProvider;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class HostApiServiceImpl implements HostApiService {

    private static final String HOST_UUID = "hostUuid";

    private static final DynamicStringProperty HEADER_AUTH = ArchaiusUtil.getString("host.api.auth.header");
    private static final DynamicStringProperty HEADER_AUTH_VALUE = ArchaiusUtil.getString("host.api.auth.header.value");

    ObjectManager objectManager;
    TokenService tokenService;
    HostApiRSAKeyProvider keyProvider;

    public HostApiServiceImpl(ObjectManager objectManager, TokenService tokenService, HostApiRSAKeyProvider keyProvider) {
        super();
        this.objectManager = objectManager;
        this.tokenService = tokenService;
        this.keyProvider = keyProvider;
    }

    @Override
    public HostApiAccess getAccess(ApiRequest request, Long hostId, Map<String, Object> data, String... resourcePathSegments) {
        return getAccess(request, hostId, data, null, resourcePathSegments);
    }

    @Override
    public HostApiAccess getAccess(ApiRequest request, Long hostId, Map<String, Object> data, Date expiration, String... resourcePathSegments) {
        Host host = objectManager.loadResource(Host.class, hostId);
        if (host == null) {
            return null;
        }

        String token = getToken(host, data, expiration);
        if (token == null) {
            return null;
        }

        Map<String, String> values = new HashMap<>();
        values.put(HEADER_AUTH.get(), String.format(HEADER_AUTH_VALUE.get(), token));

        return new HostApiAccess(getHostAccessUrl(request, host, resourcePathSegments), token, values);
    }

    protected String getHostAccessUrl(ApiRequest request, Host host, String... segments) {
        StringBuilder buffer = new StringBuilder();
        switch (getHostApiProxyMode()) {
        case HOST_API_PROXY_MODE_HA:
            String proxyHost = HostApiUtils.HOST_API_PROXY_HOST.get();
            if (StringUtils.isNotBlank(proxyHost)) {
                String scheme = StringUtils.startsWithIgnoreCase(request.getResponseUrlBase(), "https") ? "wss://" : "ws://";
                buffer.append(scheme).append(proxyHost);
                break;
            }
            // Purposefully fall through
        case HOST_API_PROXY_MODE_EMBEDDED:
            String url = request.getResponseUrlBase().replaceFirst("http", "ws");
            buffer.append(url);
            break;
        case HOST_API_PROXY_MODE_OFF:
            throw new ClientVisibleException(501, "HostApiProxyDisabled");
        }

        if (buffer.length() <= 0) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "CantConstructUrl");
        }

        if (segments != null) {
            for (String segment : segments) {
                if (buffer.charAt(buffer.length() -1) != '/' && !segment.startsWith("/")) {
                    buffer.append("/");
                }
                buffer.append(segment);
            }
        }

        return buffer.toString();
    }

    @Override
    public Map<String, PublicKey> getPublicKeys() {
        return keyProvider.getPublicKeys();
    }

    protected String getToken(Host host, Map<String, Object> inputData, Date expiration) {
        Map<String, Object> data = new HashMap<>(inputData);
        String uuid = host.getExternalId();
        if (uuid != null) {
            data.put(HOST_UUID, uuid);
        } else {
            data.put(HOST_UUID, host.getUuid());
        }

        if (expiration == null) {
            return tokenService.generateToken(data);
        } else {
            return tokenService.generateToken(data, expiration);
        }
    }

}
