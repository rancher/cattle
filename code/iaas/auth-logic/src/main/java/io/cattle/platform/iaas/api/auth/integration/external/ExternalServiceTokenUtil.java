package io.cattle.platform.iaas.api.auth.integration.external;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.AbstractTokenUtil;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class ExternalServiceTokenUtil extends AbstractTokenUtil {

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

    public Identity jsonToIdentity(Map<String, Object> jsonData) {
        String externalId = ObjectUtils.toString(jsonData.get("externalId"));
        String externalIdType = ObjectUtils.toString(jsonData.get("externalIdType"));
        String name = ObjectUtils.toString(jsonData.get("name"));
        String profilePicture = ObjectUtils.toString(jsonData.get("profilePicture"));
        String profileUrl = ObjectUtils.toString(jsonData.get("profileUrl"));
        String login = ObjectUtils.toString(jsonData.get("login"));
        return new Identity(externalIdType, externalId, name, profileUrl, profilePicture, login);
    }

    public Token getUserIdentityFromJWT() {
        Token token = new Token();
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return null;
        }
        Object idObject = (Object)jsonData.get(USER_IDENTITY);
        if (idObject != null) {
            Map<String, Object> idMap = CollectionUtils.toMap(idObject);
            Identity userIdentity = jsonToIdentity(idMap);
            String userType = ObjectUtils.toString(jsonData.get(USER_TYPE), null);
            token.setUserIdentity(userIdentity);
            token.setUserType(userType);
        }
        return token;
    }
}
