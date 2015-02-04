package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.jooq.AbstractJooqResourceManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.impl.AccountPolicy;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class ProjectResourceManager extends AbstractJooqResourceManager {

    private static final String TEAM_SCOPE = "project:github_team";
    private static final String ORG_SCOPE = "project:github_org";
    private static final String USER_SCOPE = "project:github_user";
    private static final String AUTH = "Authorization";
    private static final String EXTERNAL_ID = "externalId";
    private static final String EXTERNAL_ID_TYPE = "externalIdType";

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
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        GithubUtils.AccesibleIds ids = githubUtils.validateAndFetchAccesibleIdsFromToken(token);
        String userId = ids.getUserId();
        List<String> teamIds = ids.getTeamIds();
        List<String> orgIds = ids.getOrgIds();
        List<? extends Account> projects = authDao.getAccessibleProjects(userId, orgIds, teamIds);
        return transformProjects(projects, token);

    }

    private List<Account> transformProjects(List<? extends Account> projects, String token) {
        if (projects == null) {
            return null;
        }

        GithubUtils.ReverseMappings reverseMappings = githubUtils.validateAndFetchReverseMappings(token);
        Map<String, String> orgsMap = reverseMappings.getOrgMap();
        String username = reverseMappings.getUsername();

        List<Account> transformedProjects = new ArrayList<>(projects.size());
        for (Account project : projects) {
            if (StringUtils.equals(project.getExternalIdType(), ORG_SCOPE)) {
                transformedProjects.add(copyProjectWithExternalId(project, orgsMap.get(project.getExternalId())));
            } else if (StringUtils.equals(project.getExternalIdType(), USER_SCOPE)) {
                transformedProjects.add(copyProjectWithExternalId(project, username));
            } else {
                transformedProjects.add(project);
            }
        }
        for(Account project: transformedProjects) {
            Policy policy = (Policy) ApiContext.getContext().getPolicy();
            policy.grantObjectAccess(project);
        }
        return transformedProjects;
    }

    private Account copyProjectWithExternalId(Account account, String externalId) {
        if (null == account) {
            return account;
        }
        if (StringUtils.isNotEmpty(externalId)) {
            account.setExternalId(externalId);
        }
        return account;
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        if (!AccountConstants.TYPE_PROJECT.equals(type)) {
            return null;
        }

        return createProject(request);
    }

    private Account createProject(ApiRequest apiRequest) {
        HttpServletRequest request = apiRequest.getServletContext().getRequest();
        String token = request.getHeader(AUTH);
        if (StringUtils.isEmpty(token)) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
        }
        Map<String, Object> account = CollectionUtils.toMap(apiRequest.getRequestObject());
        String externalId = getExternalId((String) account.get(EXTERNAL_ID), (String) account.get(EXTERNAL_ID_TYPE), token);
        if (StringUtils.isEmpty(externalId)) {
            return null;
        }
        Account newAccount = authDao.createAccount((String) account.get("name"), AccountConstants.TYPE_PROJECT, externalId,
                (String) account.get(EXTERNAL_ID_TYPE));
        AccountPolicy policy = (AccountPolicy) ApiContext.getContext().getPolicy();
        Account modifiedAccount = copyProjectWithExternalId(newAccount, (String) account.get(EXTERNAL_ID));
        policy.grantObjectAccess(modifiedAccount);
        return modifiedAccount;
    }

    private String getExternalId(String externalId, String externalIdType, String jwt) {
        String token = githubUtils.validateAndFetchGithubToken(jwt);
        if (StringUtils.equals(externalIdType, TEAM_SCOPE)) {
            if (StringUtils.isEmpty(externalId)) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.MISSING_REQUIRED,
                        "externalId for TEAM scope should not be null", null);
            }
            return externalId;
        } else if (StringUtils.equals(externalIdType, ORG_SCOPE)) {
            try {
                GithubAccountInfo accountInfo = githubClient.getOrgIdByName(externalId, token);
                if (null == accountInfo) {
                    return null;
                }
                return accountInfo.getAccountId();
            } catch (IOException e) {
                throw new ClientVisibleException(ResponseCodes.SERVICE_UNAVAILABLE, "GithubUnavailable", "could not retrieve orgId from github", null);
            }
        } else if (StringUtils.equals(externalIdType, USER_SCOPE)) {
            return githubUtils.validateAndFetchAccountIdFromToken(jwt);
        }
        throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, "UnrecognizedScope", "Scope " + externalIdType + " is invalid", null);
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
