package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.integration.external.ExternalServiceTokenUtil;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenUtil;
import io.cattle.platform.util.type.NamedUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class TokenAuthLookup implements AccountLookup, Priority {

    Map<String, TokenUtil> tokenUtilsMap = new HashMap<String, TokenUtil>();
    @Inject
    List<TokenUtil> tokenUtils;

    @Inject
    ExternalServiceTokenUtil externalTokenUtil;

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @PostConstruct
    public void init() {
        for (TokenUtil tu : tokenUtils) {
            tokenUtilsMap.put(NamedUtils.getName(tu), tu);
        }
    }

    @Override
    public Account getAccount(ApiRequest request) {
        if (!StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType()) && isConfigured()) {
            tokenUtils().findAndSetJWT();
            return getAccountAccessInternal();
        }
        return null;
    }

    private Account getAccountAccessInternal() {
        return tokenUtils().getAccountFromJWT();
    }

    public Account getAccountAccess(String token, ApiRequest request) {
        if (!StringUtils.equals(AbstractTokenUtil.TOKEN, request.getType()) && isConfigured()) {
            request.setAttribute(tokenUtils().tokenType(), token);
            return getAccountAccessInternal();
        }
        return null;
    }

    private TokenUtil tokenUtils(){
        if (SecurityConstants.INTERNAL_AUTH_PROVIDERS.contains(SecurityConstants.AUTH_PROVIDER.get())) {
            TokenUtil tokenUtil = tokenUtilsMap.get(SecurityConstants.AUTH_PROVIDER.get());
            if (tokenUtil == null || !tokenUtil.isConfigured()) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "TokenUtilNotConfigured");
            }
            return tokenUtil;
        } else {
            return externalTokenUtil;
        }
    }

    @Override
    public boolean challenge(ApiRequest request) {
        return false;
    }

    @Override
    public String getName() {
        return "TokenAuth";
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.isNotBlank(SecurityConstants.AUTH_PROVIDER.get())
                && !SecurityConstants.NO_PROVIDER.equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get());
    }

}
