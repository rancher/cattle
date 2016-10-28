package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AuthToken;
import io.cattle.platform.core.util.SettingsUtils;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.dao.AuthTokenDao;
import io.cattle.platform.iaas.api.auth.identity.Token;
import io.cattle.platform.iaas.api.auth.integration.interfaces.TokenUtil;
import io.cattle.platform.iaas.api.auth.projects.ProjectResourceManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTokenUtil implements TokenUtil {

    public static final String ACCESSMODE = "accessMode";
    public static final String TOKEN = "token";
    public static final String ACCOUNT_ID = "account_id";
    public static final String ACCESS_TOKEN_INVALID = "InvalidAccessToken";
    public static final String ID_LIST = "idList";
    public static final String USER_IDENTITY = "userIdentity";
    public static final String USER_TYPE = "userType";

    public static final String REQUIRED_ACCESSMODE = "required";
    public static final String RESTRICTED_ACCESSMODE = "restricted";
    public static final String UNRESTRICTED_ACCESSMODE = "unrestricted";

    private static final Logger log = LoggerFactory.getLogger(AbstractTokenUtil.class);

    @Inject
    protected AuthDao authDao;

    @Inject
    TokenService tokenService;

    @Inject
    ProjectResourceManager projectResourceManager;

    @Inject
    AuthTokenDao authTokenDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    SettingsUtils settingsUtils;

    @Inject
    AccountDao accountDao;

    @Override
    public Account getAccountFromJWT() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return null;
        }
        String accountId = ObjectUtils.toString(jsonData.get(ACCOUNT_ID), null);
        if (null == accountId) {
            return null;
        }
        Account account = authDao.getAccountByExternalId(accountId, userType());
        if (account != null && !accountDao.isActiveAccount(account)) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return account;
    }

    protected Map<String, Object> getJsonData() {
        return getJsonData(getJWT(), tokenType());
    }

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

    @Override
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
        log.trace("ID List in the token: {}", idList);
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
            Identity identityObj = Identity.fromId(id);
            if (identityObj != null) {
                identities.add(identityObj);
            } else {
                log.trace("Identity is null for id: {}", id);
            }
        }
        return identities;
    }

    @Override
    public Set<Identity> getIdentities() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return new HashSet<>();
        }
        return identities(jsonData);
    }

    @Override
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

    @Override
    public boolean isAllowed(List<String> idList, Set<Identity> identities) {
        switch (accessMode()) {
            case REQUIRED_ACCESSMODE:
                if (isWhitelisted(idList)) {
                    break;
                }
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            case RESTRICTED_ACCESSMODE:
                boolean hasAccessToAProject = authDao.hasAccessToAnyProject(identities, false, null);
                if (hasAccessToAProject || isWhitelisted(idList)) {
                    break;
                }
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            case UNRESTRICTED_ACCESSMODE:
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return true;
    }

    protected abstract boolean isWhitelisted(List<String> idList);

    protected abstract String accessMode();

    @Override
    public String getAccessToken() {
        if (findAndSetJWT()) {
            Account account = getAccountFromJWT();
            return account != null ? (String) DataAccessor.fields(account).withKey(accessToken()).get() : null;
        } else {
            return null;
        }
    }

    @Override
    public List<String> identitiesToIdList(Set<Identity> identities){
        List<String> idList = new ArrayList<>();
        for (Identity identity: identities){
            idList.add(identity.getId());
        }
        return idList;
    }

    protected abstract String accessToken();

    @Override
    public Account getOrCreateAccount(Identity user, Set<Identity> identities, Account account) {
        if (SecurityConstants.SECURITY.get()) {
            isAllowed(identitiesToIdList(identities), identities);
            if (account == null) {
                account = authDao.getAccountByExternalId(user.getExternalId(), user.getExternalIdType());
            }
            if (account != null && !accountDao.isActiveAccount(account)) {
                throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
            }
            if (account == null && createAccount()) {
                account = authDao.createAccount(user.getName(), AccountConstants.USER_KIND, user
                                .getExternalId(),
                        user.getExternalIdType());
            }
            Object hasLoggedIn = DataAccessor.fields(account).withKey(SecurityConstants.HAS_LOGGED_IN).get();
            if ((hasLoggedIn == null || !((Boolean) hasLoggedIn)) &&
                    !authDao.hasAccessToAnyProject(identities, false, null)) {
                projectResourceManager.createProjectForUser(user);
            }
        } else {
            if (account == null) {
                account = authDao.getAccountByExternalId(user.getExternalId(), user.getExternalIdType());
            }
            if (account != null){
                account.setKind(AccountConstants.ADMIN_KIND);
                objectManager.persist(account);
            } else {
                account = authDao.getAdminAccount();
            }
            authDao.ensureAllProjectsHaveNonRancherIdMembers(user);
            settingsUtils.changeSetting(SecurityConstants.AUTH_ENABLER, user.getId());
        }
        if (account != null) {
            DataAccessor.fields(account).withKey(SecurityConstants.HAS_LOGGED_IN).set(true);
            objectManager.persist(account);
        }
        return account;
    }


    @Override
    public Token createToken(Set<Identity> identities, Account account) {

        Identity user = getUser(identities);

        if (user == null) {
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }

        account = getOrCreateAccount(user, identities, account);

        if (account == null){
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "FailedToGetAccount");
        }

        postAuthModification(account);

        account = authDao.updateAccount(account, user.getName(), account.getKind(), user.getExternalId(), user
                .getExternalIdType());

        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put(AbstractTokenUtil.TOKEN, tokenType());
        jsonData.put(AbstractTokenUtil.ACCOUNT_ID, user.getExternalId());
        jsonData.put(AbstractTokenUtil.ID_LIST, identitiesToIdList(identities));
        jsonData.put(AbstractTokenUtil.USER_IDENTITY, user);
        jsonData.put(AbstractTokenUtil.USER_TYPE, account.getKind());

        String accountId = (String) ApiContext.getContext().getIdFormatter().formatId(objectManager.getType(Account.class), account.getId());
        Date expiry = new Date(System.currentTimeMillis() + SecurityConstants.TOKEN_EXPIRY_MILLIS.get());
        String jwt = tokenService.generateEncryptedToken(jsonData, expiry);
        return new Token(jwt, accountId, user, new ArrayList<>(identities), account.getKind());

    }

    protected abstract void postAuthModification(Account account);

    @Override
    public Identity getUser(Set<Identity> identities) {
        for (Identity identity: identities){
            if (identity != null && identity.getExternalIdType().equalsIgnoreCase(userType())){
                return identity;
            }
        }
        return null;
    }

    @Override
    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.isNotBlank(SecurityConstants.AUTH_PROVIDER.get())
                && getName().equalsIgnoreCase(SecurityConstants.AUTH_PROVIDER.get());
    }

    public static boolean isRestrictedAccess(String accessMode){
        return RESTRICTED_ACCESSMODE.equalsIgnoreCase(accessMode);
    }

    public static boolean isRequiredAccess(String accessMode){
        return REQUIRED_ACCESSMODE.equalsIgnoreCase(accessMode);
    }

    public static boolean isUnrestrictedAccess(String accessMode){
        return UNRESTRICTED_ACCESSMODE.equalsIgnoreCase(accessMode);
    }

}
