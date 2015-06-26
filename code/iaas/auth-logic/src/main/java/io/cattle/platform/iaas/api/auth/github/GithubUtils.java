package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.constants.GithubConstants;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.Cookie;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.netflix.config.DynamicStringProperty;

public class GithubUtils {

    private static final DynamicStringProperty ACCESS_MODE = ArchaiusUtil.getString("api.auth.github.access.mode");
    private static final DynamicStringProperty WHITELISTED_ORGS = ArchaiusUtil.getString("api.auth.github.allowed.orgs");
    private static final DynamicStringProperty WHITELISTED_USERS = ArchaiusUtil.getString("api.auth.github.allowed.users");

    private TokenService tokenService;


    @Inject
    GithubClient githubClient;
    @Inject
    AuthDao authDao;

    public Account getAccountFromJWT() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null) {
            return null;
        }
        String accountId = ObjectUtils.toString(jsonData.get(GithubConstants.ACCOUNT_ID), null);
        if (null == accountId) {
            return null;
        }
        return authDao.getAccountByExternalId(accountId, GithubConstants.USER_SCOPE);
    }

    public Set<ExternalId> externalIds() {
        Map<String, Object> jsonData = getJsonData();
        if (jsonData == null){
            return new HashSet<>();
        }
        return externalIds(jsonData);
    }

    @SuppressWarnings("unchecked")
    private Set<ExternalId> externalIds(Map<String, Object> jsonData) {
        Set<ExternalId> externalIds = new HashSet<>();
        if (jsonData == null){
            return externalIds;
        }
        List<String> teamIds = (List<String>) CollectionUtils.toList(jsonData.get(GithubConstants.TEAM_IDS));
        List<String> orgIds = (List<String>) CollectionUtils.toList(jsonData.get(GithubConstants.ORG_IDS));
        String accountId = ObjectUtils.toString(jsonData.get(GithubConstants.ACCOUNT_ID), null);
        externalIds.add(new ExternalId(accountId , GithubConstants.USER_SCOPE, (String) jsonData.get(GithubConstants.USERNAME)));
        for (String teamId: teamIds){
            externalIds.add(new ExternalId(teamId, GithubConstants.TEAM_SCOPE));
        }
        for (String orgId: orgIds){
            externalIds.add(new ExternalId(orgId, GithubConstants.ORG_SCOPE));
        }
        return  externalIds;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonData(String jwt) {
        if (StringUtils.isEmpty(jwt)) {
            return null;
        }
        String toParse;
        String[] tokenArr = jwt.split("\\s+");
        if (tokenArr.length == 2) {
            if (!StringUtils.equalsIgnoreCase("bearer", StringUtils.trim(tokenArr[0]))) {
                return null;
            }
            toParse = tokenArr[1];
        } else if (tokenArr.length == 1) {
            toParse = tokenArr[0];
        } else {
            toParse = jwt;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        if (jsonData == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.MISSING_REQUIRED,
                    "There is no github token present. Or it is invalid.", null);
        }
        List<String> idList = (List<String>) jsonData.get(GithubConstants.ID_LIST);
        Set<ExternalId> externalIds = externalIds(jsonData);
        if (!isAllowed(idList, externalIds)){
            throw new ClientVisibleException(ResponseCodes.UNAUTHORIZED);
        }
        return jsonData;
    }

    private Map<String, Object> getJsonData(){
        return getJsonData(getJWT());
    }

    public String getJWT(){
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(GithubConstants.GITHUB_JWT);
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt) == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_FORMAT,
                    "Token malformed after retrieval.", null);
        }
        return jwt;
    }
    public void findAndSetJWT() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String jwt = (String) request.getAttribute(GithubConstants.GITHUB_JWT);
        if (StringUtils.isNotBlank(jwt) && getJsonData(jwt) != null){
            return;
        }
        if (StringUtils.isBlank(jwt)) {
            if (request.getServletContext().getRequest().getCookies() != null) {
                for (Cookie cookie : request.getServletContext().getRequest().getCookies()) {
                    if (cookie.getName().equalsIgnoreCase(GithubConstants.TOKEN) 
                            && StringUtils.isNotBlank(cookie.getValue())) {
                        jwt = cookie.getValue();
                    }
                }
            }
        }
        if (StringUtils.isBlank(jwt)){
            jwt = request.getServletContext().getRequest().getHeader(ProjectConstants.AUTH_HEADER);
        }
        if (StringUtils.isBlank(jwt)){
            jwt = request.getServletContext().getRequest().getParameter(GithubConstants.TOKEN);
        }
        if (getJsonData(jwt) != null){
            request.setAttribute(GithubConstants.GITHUB_JWT, jwt);
        }
    }

    @Inject
    public void setJwtTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Set<ExternalId> getExternalIds() {
        return externalIds();
    }

    @SuppressWarnings("unchecked")
    public String getTeamOrgById(String id) {
        Map<String, Object> jsonData = getJsonData();
        Map<String, String> teamToOrg = (Map<String, String>) jsonData.get("teamToOrg");
        return  teamToOrg.get(id);
    }

    public boolean isAllowed(List<String> idList, Set<ExternalId> externalIds){
        boolean hasAccessToAProject = authDao.hasAccessToAnyProject(externalIds, false, null);
        switch (ACCESS_MODE.get()) {
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

    private boolean isWhitelisted(List<String> idList) {
        if (idList == null || idList.isEmpty()) {
            return false;
        }
        List<String> whitelistedValues = fromCommaSeparatedString(WHITELISTED_ORGS.get());
        whitelistedValues.addAll(fromCommaSeparatedString(WHITELISTED_USERS.get()));
        Collection<String> whitelistedIds = Collections2.transform(whitelistedValues, new Function<String, String>() {
            @Override
            public String apply(String arg) {
                return arg.split("[:]")[1];
            }
        });
        Set<String> whitelist = new HashSet<>(whitelistedIds);
        for (String id : idList) {
            if (whitelist.contains(id)) {
                return true;
            }
        }
        return false;
    }

    private List<String> fromCommaSeparatedString(String string) {
        if (StringUtils.isEmpty(string)) {
            return new ArrayList<>();
        }
        List<String> strings = new ArrayList<String>();
        String[] splitted = string.split(",");
        for (String aSplitted : splitted) {
            String element = aSplitted.trim();
            strings.add(element);
        }
        return strings;
    }

    public String getURL(GithubClientEndpoints val) {
        String hostName;
        String apiEndpoint;
        if (StringUtils.isBlank(GithubConstants.GITHUB_HOSTNAME.get())) {
            hostName = GithubConstants.GITHUB_DEFAULT_HOSTNAME;
            apiEndpoint = GithubConstants.GITHUB_API;
        } else {
            hostName = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get();
            apiEndpoint = GithubConstants.SCHEME.get() + GithubConstants.GITHUB_HOSTNAME.get() + GithubConstants.GHE_API;
        }
        String toReturn;
        switch (val) {
            case API:
                toReturn = apiEndpoint;
                break;
            case TOKEN:
                toReturn = hostName + "/login/oauth/access_token";
                break;
            case USERS:
                toReturn = apiEndpoint + "/users/";
                break;
            case ORGS:
                toReturn = apiEndpoint + "/orgs/";
                break;
            case USER_INFO:
                toReturn = apiEndpoint + "/user";
                break;
            case ORG_INFO:
                toReturn = apiEndpoint + "/user/orgs";
                break;
            default:
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "GithubClient", "Attempted to get invalid Api endpoint.", null);
        }
        return toReturn;
    }

    public String getAccessToken() {
        findAndSetJWT();
        return (String) DataAccessor.fields(getAccountFromJWT()).withKey(GithubConstants.GITHUB_ACCESS_TOKEN).get();
    }
}
