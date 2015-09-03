package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class TokenUtils  {

    public static final String ACCESSMODE = "accessMode";
    public static final String TOKEN = "token";
    public static final String ACCOUNT_ID = "account_id";
    public static final String ACCESS_TOKEN_INVALID = "InvalidAccessToken";
    public static final String ID_LIST = "idList";


    @Inject
    protected AuthDao authDao;

    @Inject
    TokenService tokenService;

    @Inject
    ProjectResourceManager projectResourceManager;

    @Inject
    AuthTokenDao authTokenDao;

    public Account getAccountFromJWT() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return null;
        }
        String accountId = ObjectUtils.toString(jsonData.get(ACCOUNT_ID), null);
        if (null == accountId) {
            return null;
        }
        return authDao.getAccountByExternalId(accountId, getAccountType());
    }

    protected abstract String getAccountType();

    protected Map<String, Object> getJsonData() {
        return getJsonData(getJWT(), tokenType());
    }

    protected abstract String tokenType();

    private Map<String, Object> getJsonData(String jwtKey, String tokenType) {
        if (StringUtils.isEmpty(jwtKey)){
            return null;
        }
        String toParse;
        toParse = removeBearer(jwtKey);
        if (StringUtils.isBlank(toParse)) {
            return null;
        }
        String dbJwt = retrieveJwt(toParse);
        if (StringUtils.isNotBlank(dbJwt)){
            toParse = dbJwt;
        }
        toParse = removeBearer(toParse);
        if (StringUtils.isEmpty(toParse)) {
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        if (jsonData == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ACCESS_TOKEN_INVALID,
                    "Json Web Token invalid.", null);
        }
        String tokenTypeActual = (String) jsonData.get(TOKEN);
        if (!StringUtils.equals(tokenType, tokenTypeActual)) {
            return null;
        }
        if (!isAllowed(jsonData)) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return jsonData;
    }

    private String removeBearer(String jwtKey) {
        String toParse;
        String[] tokenArr = jwtKey.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            toParse = jwtKey;
        }
        return toParse;
    }

    private String retrieveJwt(String jwtKey) {
        AuthToken authToken= authTokenDao.getTokenByKey(jwtKey);
        if (authToken == null) {
            return null;
        }
        if (!authToken.getProvider().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get())){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED, "AuthProviderChanged",
                    "Access control has changed since token was created.", null);
        }
        return authToken.getValue();
    }

    public String getJWT() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(tokenType());
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt, tokenType()) == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_FORMAT,
                    "Token malformed after retrieval.", null);
        }
        return jwt;
    }

    @SuppressWarnings("unchecked")
    protected boolean isAllowed(Map<String, Object> jsonData) {
        List<String> idList = (List<String>) jsonData.get(ID_LIST);
        Set<Identity> identities = identities(jsonData);
        return isAllowed(idList, identities);
    }

    @SuppressWarnings("unchecked")
    protected Set<Identity> identities(Map<String, Object> jsonData) {
        Set<Identity> identities = new HashSet<>();
        if (jsonData == null) {
            return identities;
        }
        List<String> idList = (List<String>) jsonData.get(ID_LIST);
        for (String id : idList) {
            identities.add(Identity.fromId(id));
        }
        return identities;
    }

    public Set<Identity> getIdentities() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return new HashSet<>();
        }
        return identities(jsonData);
    }

    public boolean findAndSetJWT() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(tokenType());
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt, tokenType()) != null) {
            return true;
        }
        if (StringUtils.isBlank(jwt)) {
            Cookie[] cookies = request.getServletContext().getRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(TOKEN)
                            && StringUtils.isNotBlank(cookie.getValue())) {
                        jwt = cookie.getValue();
                        break;
                    }
                }
            }
        }
        if (StringUtils.isBlank(jwt)) {
            jwt = request.getServletContext().getRequest().getHeader(ProjectConstants.AUTH_HEADER);
        }
        if (StringUtils.isBlank(jwt)) {
            jwt = request.getServletContext().getRequest().getParameter(TOKEN);
        }
        if (getJsonData(jwt, tokenType()) != null) {
            request.setAttribute(tokenType(), jwt);
            return true;
        }
        return false;
    }

    public boolean isAllowed(List<String> idList, Set<Identity> identities) {
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        switch (accessMode()) {
            case "restricted":
                if (hasAccessToAProject || isWhitelisted(idList)) {
                    break;
                }
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            case "unrestricted":
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return true;
    }

    protected abstract boolean isWhitelisted(List<String> idList);

    protected abstract String accessMode();

    public String getAccessToken() {
        if (findAndSetJWT()) {
            Account account = getAccountFromJWT();
            if (account == null) {
                return null;
            }
            return (String) DataAccessor.fields(account).withKey(accessToken()).get();
        } else {
            return null;
        }
    }

    public List<String> identitiesToIdList(Set<Identity> identities){
        List<String> idList = new ArrayList<>();
        for (Identity identity: identities){
            idList.add(identity.getId());
        }
        return idList;
    }

    protected abstract String accessToken();

    public Account getOrCreateAccount(Identity gotIdentity, Set<Identity> identities, Account account, boolean createAccount) {
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
        if (SecurityConstants.SECURITY.get()) {
            isAllowed(identitiesToIdList(identities), identities);
            if (account == null) {
            account = authDao.getAccountByExternalId(gotIdentity.getExternalId(), gotIdentity.getExternalIdType());
            }
            if (account == null && createAccount) {
                account = authDao.createAccount(gotIdentity.getName(), AccountConstants.USER_KIND, gotIdentity
                                .getExternalId(),
                        gotIdentity.getExternalIdType());
            }
            Object hasLoggedIn = DataAccessor.fields(account).withKey(SecurityConstants.HAS_LOGGED_IN).get();
            if (!hasAccessToAProject && hasLoggedIn != null && !((Boolean) hasLoggedIn)) {
                projectResourceManager.createProjectForUser(account);
            }
        } else {
            account = authDao.getAdminAccount();
            authDao.updateAccount(account, null, AccountConstants.ADMIN_KIND, gotIdentity.getExternalId(), gotIdentity.getExternalIdType());
            authDao.ensureAllProjectsHaveNonRancherIdMembers(gotIdentity);
        }
        if (account != null) {
            DataAccessor.fields(account).withKey(SecurityConstants.HAS_LOGGED_IN).set(true);
        }
        return account;
    }
}
