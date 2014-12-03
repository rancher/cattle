package io.cattle.platform.host.service.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.host.model.HostApiAccess;
import io.cattle.platform.host.service.HostApiRSAKeyProvider;
import io.cattle.platform.host.service.HostApiService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.token.TokenService;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringProperty;

public class HostApiServiceImpl implements HostApiService {

    private static final String HOST_UUID = "hostUuid";

    private static final DynamicStringProperty HEADER_AUTH = ArchaiusUtil.getString("host.api.auth.header");
    private static final DynamicStringProperty HEADER_AUTH_VALUE = ArchaiusUtil.getString("host.api.auth.header.value");

    ObjectManager objectManager;
    TokenService tokenService;
    HostApiRSAKeyProvider keyProvider;

    @Override
    public HostApiAccess getAccess(Long hostId) {
        return getAccess(hostId, new HashMap<String,Object>());
    }

    @Override
    public HostApiAccess getAccess(Long hostId, Map<String,Object> data) {
        Host host = objectManager.loadResource(Host.class, hostId);
        if ( host == null ) {
            return null;
        }

        IpAddress ip = getIpAddress(host);
        if ( ip == null || ip.getAddress() == null ) {
            return null;
        }

        String token = getToken(host, data);
        if ( token == null ) {
            return null;
        }

        Map<String,String> values = new HashMap<String, String>();
        values.put(HEADER_AUTH.get(), String.format(HEADER_AUTH_VALUE.get(), token));

        return new HostApiAccess(ip.getAddress(), token, values);
    }

    @Override
    public Map<String, PublicKey> getPublicKeys() {
        return keyProvider.getPublicKeys();
    }


    protected String getToken(Host host, Map<String,Object> inputData) {
        Map<String,Object> data = new HashMap<String,Object>(inputData);
        data.put(HOST_UUID, host.getUuid());

        return tokenService.generateToken(data);
    }

    protected IpAddress getIpAddress(Host host) {
        IpAddress choice = null;

        for ( IpAddress ip : objectManager.mappedChildren(host, IpAddress.class) ) {
            if ( ip.getAddress() == null || ! CommonStatesConstants.ACTIVE.equals(ip.getState()) ) {
                continue;
            }

            if ( IpAddressConstants.ROLE_PRIMARY.equals(ip.getRole()) ) {
                choice = ip;
                break;
            } else if ( choice == null || choice.getCreated() == null ) {
                choice = ip;
            } else if ( ip.getCreated() != null && ip.getCreated().before(choice.getCreated()) ) {
                choice = ip;
            }
        }

        return choice;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public TokenService getTokenService() {
        return tokenService;
    }

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public HostApiRSAKeyProvider getKeyProvider() {
        return keyProvider;
    }

    @Inject
    public void setKeyProvider(HostApiRSAKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

}
