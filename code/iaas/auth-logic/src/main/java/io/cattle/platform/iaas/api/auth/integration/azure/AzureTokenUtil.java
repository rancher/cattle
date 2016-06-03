package io.cattle.platform.iaas.api.auth.integration.azure;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class AzureTokenUtil extends AbstractTokenUtil {

    @Override
    protected String accessMode() {
        return AzureConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return AzureConstants.AZURE_ACCESS_TOKEN;
    }

    @Override
    protected void postAuthModification(Account account) {
        if(account != null) {
            ApiRequest request = ApiContext.getContext().getApiRequest();
            String accessToken = (String) request.getAttribute(AzureConstants.AZURE_ACCESS_TOKEN);
            String refreshToken = (String) request.getAttribute(AzureConstants.AZURE_REFRESH_TOKEN);
            DataAccessor.fields(account).withKey(AzureConstants.AZURE_ACCESS_TOKEN).set(accessToken);
            DataAccessor.fields(account).withKey(AzureConstants.AZURE_REFRESH_TOKEN).set(refreshToken);
            getObjectManager().persist(account);
        }
    }

    protected void refreshAccessToken() {
        if (findAndSetJWT()) {
            Account account = getAccountFromJWT();
            postAuthModification(account);
        }
    }

    @Override
    public String userType() {
        return AzureConstants.USER_SCOPE;
    }

    @Override
    public boolean createAccount() {
        return true;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        //TODO Real white listing needed.
        return true;
    }

    public List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<>();
        String[] splitted = string.split(",");
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
    }

    @Override
    public String tokenType() {
        return AzureConstants.AZURE_JWT;
    }

    @Override
    public String getName() {
        return AzureConstants.CONFIG;
    }
}
