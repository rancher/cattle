package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.ExternalId;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.token.TokenException;
import io.cattle.platform.token.TokenService;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class GithubUtils {

    public static final String TEAM_SCOPE = "github_team";
    public static final String ORG_SCOPE = "github_org";
    public static final String USER_SCOPE = "github_user";
    private TokenService tokenService;


    @Inject
    GithubClient githubClient;

    public String validateAndFetchGithubToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (jsonData == null) {
            return null;
        }
        return ObjectUtils.toString(jsonData.get("access_token"), null);
    }

    public String validateAndFetchAccountIdFromToken(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        if (jsonData == null) {
            return null;
        }
        return ObjectUtils.toString(jsonData.get("account_id"), null);
    }

    @SuppressWarnings("unchecked")
    public Set<ExternalId> externalIds(String token) {
        Map<String, Object> jsonData = getJsonData(token);
        List<String> teamIds = (List<String>) CollectionUtils.toList(jsonData.get("team_ids"));
        List<String> orgIds = (List<String>) CollectionUtils.toList(jsonData.get("org_ids"));
        String accountId = ObjectUtils.toString(jsonData.get("account_id"), null);
        Set<ExternalId> externalIds = new HashSet<>();
        externalIds.add(new ExternalId(accountId , USER_SCOPE, (String) jsonData.get("username")));
        for (String teamId: teamIds){
            externalIds.add(new ExternalId(teamId, TEAM_SCOPE));
        }
        for (String orgId: orgIds){
            externalIds.add(new ExternalId(orgId, ORG_SCOPE));
        }
        return  externalIds;
    }

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
            return null;
        }
        Map<String, Object> jsonData;
        try {
            jsonData = tokenService.getJsonPayload(toParse, true);
        } catch (TokenException e) { // in case of invalid token
            return null;
        }
        if (jsonData == null){
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.MISSING_REQUIRED,
                    "There is no github token present.", null);
        }
        return jsonData;
    }

    @Inject
    public void setJwtTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Set<ExternalId> getExternalIds() {
        Set<ExternalId> externalIds = externalIds(getToken());
        return externalIds;
    }

    public String getToken() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        String token = request.getServletContext().getRequest().getHeader(ProjectConstants.AUTH_HEADER);
        if (StringUtils.isEmpty(token) || !token.toLowerCase().startsWith(ProjectConstants.AUTH_TYPE)) {
            token = request.getServletContext().getRequest().getParameter("token");
            if (StringUtils.isEmpty(token)) {
                token = (String) request.getServletContext().getRequest().getAttribute(ProjectConstants.OAUTH_BASIC);
                if (token.isEmpty()) {
                    throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.MISSING_REQUIRED,
                            "There is no github token present.", null);
                }
            }
        }
        return token;
    }

    public String getTeamOrgById(String id) {
        Map<String, Object> jsonData = getJsonData(getToken());
        Map<String, String> teamToOrg = (Map<String, String>) jsonData.get("teamToOrg");
        return  teamToOrg.get(id);
    }
}
