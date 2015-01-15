package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class ProjectResourceManager extends AbstractJooqResourceManager {


    private static final String GITHUB_EXTERNAL_TYPE = "github";
    private static final String TEAM_SCOPE = "project:github_team";
    private static final String ORG_SCOPE = "project:github_org";
    private static final String USER_SCOPE = "project:github_user";
    private static final String AUTH = "Authorization";

    JsonMapper jsonMapper;
    GithubUtils githubUtils;
    GithubClient githubClient;
    AuthDao authDao;

    @Override
    public String[] getTypes() {
        return new String[] { "project" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class[] {};
    }

    @Override
    public Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        HttpServletRequest request = ApiContext.getContext().getApiRequest().getServletContext().getRequest();
        String token = request.getHeader(AUTH);
        if(StringUtils.isEmpty(token)) {
            return null;
        }
        List<String> teamIds = githubUtils.validateAndFetchTeamIdsFromToken(token);
        List<String> orgIds = githubUtils.validateAndFetchOrgIdsFromToken(token);
        String userId = githubUtils.validateAndFetchAccountIdFromToken(token);
        Account userAccount = authDao.getAccountByExternalId(userId, GITHUB_EXTERNAL_TYPE);
        if(userAccount == null) {
            return null;
        }
        return authDao.getAccessibleProjects(userAccount.getId(), orgIds, teamIds);
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!AccountConstants.PROJECT.equals(type)) {
            return null;
        }

        return createProject(request);
    }
    
    private Account createProject(ApiRequest apiRequest) {
        HttpServletRequest request = apiRequest.getServletContext().getRequest();
        String token = request.getHeader(AUTH);
        if (StringUtils.isEmpty(token)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> account = jsonMapper.convertValue(apiRequest.getRequestObject(), Map.class);
        String externalId;
        try {
            externalId = getExternalId((String) account.get("externalId"), (String) account.get("externalIdType"), token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (StringUtils.isEmpty(externalId)) {
            return null;
        }
        return authDao.createAccount((String) account.get("name"), "user", externalId, (String) account.get("externalIdType"));
    }
    
    private String getExternalId(String externalId, String externalIdType, String jwt) throws IOException {
        String token = githubUtils.validateAndFetchGithubToken(jwt);
        if (StringUtils.equals(externalIdType, TEAM_SCOPE)) {
            return externalId;
        } else if (StringUtils.equals(externalIdType, ORG_SCOPE)) {
            GithubAccountInfo accountInfo = githubClient.getOrgIdByName(externalId, token);
            if (null == accountInfo) {
                return null;
            }
            return accountInfo.getAccountId();
        } else if (StringUtils.equals(externalIdType, USER_SCOPE)) {
            String githubId = githubUtils.validateAndFetchAccountIdFromToken(jwt);
            Account userAccount = authDao.getAccountByExternalId(githubId, GITHUB_EXTERNAL_TYPE);
            return Long.toString(userAccount.getId());
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "UnrecognizedScope", "Scope " + externalIdType + "is invalid", null);
    }
    
    @Inject
    public void setGithubUtils(GithubUtils githubUtils) {
        this.githubUtils = githubUtils;
    }

    @Inject
    public void getAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
    

    @Inject
    public void setGithubClient(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

}
