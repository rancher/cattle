package io.cattle.platform.iaas.api.auth.integration.external;

import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.SettingDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ExternalServiceTokenUtil extends AbstractTokenUtil {

    public ExternalServiceTokenUtil(AuthDao authDao, TokenService tokenService, AuthTokenDao authTokenDao, ObjectManager objectManager,
                                    SettingDao settingsUtils, AccountDao accountDao) {
        super(authDao, tokenService, authTokenDao, objectManager, settingsUtils, accountDao);
    }

    @Override
    protected String accessMode() {
        return ServiceAuthConstants.ACCESS_MODE.get();
    }

    @Override
    protected String accessToken() {
        return ServiceAuthConstants.ACCESS_TOKEN;
    }

    @Override
    protected void postAuthModification(Account account) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String accessToken = (String) request.getAttribute(ServiceAuthConstants.ACCESS_TOKEN);
        DataAccessor.fields(account).withKey(ServiceAuthConstants.ACCESS_TOKEN)
                .set(accessToken);
        getObjectManager().persist(account);
    }

    @Override
    public String userType() {
        return ServiceAuthConstants.USER_TYPE.get();
    }


    public String identitySeparator() {
        return ServiceAuthConstants.IDENTITY_SEPARATOR.get();
    }

    @Override
    public boolean createAccount() {
        return true;
    }

    @Override
    protected boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromSeparatedString(ServiceAuthConstants.ALLOWED_IDENTITIES.get(), identitySeparator());

        for (String id : idList) {
            for (String whiteId: whitelistedValues){
                if (StringUtils.equals(id, whiteId)){
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> fromSeparatedString(String identities, String identitySeparator) {
        if (StringUtils.isEmpty(identities)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<>();
        String[] splitted = identities.split(identitySeparator);
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
    }

    @Override
    public String tokenType() {
        return "externaljwt";
    }

    @Override
    public String getName() {
        return "";
    }
}
