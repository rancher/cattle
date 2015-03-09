package io.cattle.platform.iaas.api.auth.github;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractObjectResourceManager;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.github.resource.GithubAccountInfo;
import io.cattle.platform.iaas.api.auth.impl.AccountPolicy;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.server.model.ApiServletContext;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

public class ProjectResourceManager extends AbstractObjectResourceManager {

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
        ApiRequest apiRequest = ApiContext.getContext().getApiRequest();
        String token = getTokenFromRequest(apiRequest);
        GithubUtils.AccesibleIds ids = githubUtils.validateAndFetchAccesibleIdsFromToken(token);
        String id = (String) criteria.get("id");
        if (StringUtils.isNotEmpty(id)) {
            Account account = getObjectManager().loadResource(Account.class, id);
            if (null == account) {
                return null;
            }
            if (!hasAccess(account, ids)) {
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
            GithubUtils.ReverseMappings reverseMappings = githubUtils.validateAndFetchReverseMappings(token);
            List<Account> result = new ArrayList<>();
            result.add(transformProject(account, reverseMappings));
            return result;
        }

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
        List<Account> transformedProjects = new ArrayList<>(projects.size());
        for (Account project : projects) {
            transformedProjects.add(transformProject(project, reverseMappings));
        }
        for (Account project : transformedProjects) {
            Policy policy = (Policy) ApiContext.getContext().getPolicy();
            policy.grantObjectAccess(project);
        }
        return transformedProjects;
    }

    private Account transformProject(Account project, GithubUtils.ReverseMappings reverseMappings) {
        if (null == project || null == reverseMappings) {
            return null;
        }
        Map<String, String> orgsMap = reverseMappings.getOrgMap();
        String username = reverseMappings.getUsername();
        if (StringUtils.equals(project.getExternalIdType(), ORG_SCOPE)) {
            return copyProjectWithExternalId(project, orgsMap.get(project.getExternalId()));
        } else if (StringUtils.equals(project.getExternalIdType(), USER_SCOPE)) {
            return copyProjectWithExternalId(project, username);
        } else {
            return project;
        }
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
        String token = getTokenFromRequest(apiRequest);
        Map<String, Object> account = CollectionUtils.toMap(apiRequest.getRequestObject());
        String externalId = getExternalId((String) account.get(EXTERNAL_ID), (String) account.get(EXTERNAL_ID_TYPE), token);
        if (StringUtils.isEmpty(externalId)) {
            return null;
        }
        Account newAccount = authDao.createAccount((String) account.get("name"), AccountConstants.TYPE_PROJECT, externalId, (String) account
                .get(EXTERNAL_ID_TYPE));
        AccountPolicy policy = (AccountPolicy) ApiContext.getContext().getPolicy();
        Account modifiedAccount = copyProjectWithExternalId(newAccount, (String) account.get(EXTERNAL_ID));
        policy.grantObjectAccess(modifiedAccount);
        return modifiedAccount;
    }

    private String getTokenFromRequest(ApiRequest apiRequest) {
        ApiServletContext apiServletContext = apiRequest.getServletContext();
        if (apiServletContext == null) {
            /* The apiServletContext will be null during any non-HTTP request call
             * to this method.  Specifically during the pub/sub operations which
             * are done in the background
             */
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
        }
        HttpServletRequest request = apiServletContext.getRequest();
        String token = request.getHeader(AUTH);
        if (StringUtils.isEmpty(token)) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
        }
        return token;
    }

    private String getExternalId(String externalId, String externalIdType, String jwt) {
        if (StringUtils.isEmpty(externalIdType)) {
            return null;
        }
        if (StringUtils.equals(externalIdType, TEAM_SCOPE)) {
            if (StringUtils.isEmpty(externalId)) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.MISSING_REQUIRED,
                        "externalId for TEAM scope should not be null", null);
            }
            List<String> teamIds = githubUtils.validateAndFetchTeamIdsFromToken(jwt);
            if (!teamIds.contains(externalId)) {
                throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
            }
            return externalId;
        } else if (StringUtils.equals(externalIdType, ORG_SCOPE)) {
            try {
                String token = githubUtils.validateAndFetchGithubToken(jwt);
                GithubAccountInfo accountInfo = githubClient.getOrgIdByName(externalId, token);
                if (null == accountInfo) {
                    throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
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

    @Override
    protected Object deleteInternal(String type, String id, final Object obj, ApiRequest apiRequest) {
        if (!(obj instanceof Account)) {
            return new Object();
        }
        String token = getTokenFromRequest(apiRequest);
        try {
            getObjectProcessManager().executeStandardProcess(StandardProcess.REMOVE, obj, null);
        } catch (ProcessCancelException e) {
            getObjectProcessManager().executeStandardProcess(StandardProcess.DEACTIVATE, obj,
                    ProcessUtils.chainInData(new HashMap<String, Object>(), AccountConstants.ACCOUNT_DEACTIVATE, AccountConstants.ACCOUNT_REMOVE));
        }
        Account deletedAccount = (Account) getObjectManager().reload(obj);
        GithubUtils.ReverseMappings reverseMappings = githubUtils.validateAndFetchReverseMappings(token);
        AccountPolicy policy = (AccountPolicy) ApiContext.getContext().getPolicy();
        Account modifiedAccount = transformProject(deletedAccount, reverseMappings);
        policy.grantObjectAccess(modifiedAccount);
        return Arrays.asList(modifiedAccount);
    }

    @Override
    protected Object removeFromStore(String type, String id, Object obj, ApiRequest apiRequest) {
        throw new UnsupportedOperationException();
    }

    private boolean hasAccess(Account account, GithubUtils.AccesibleIds ids) {
        String externalId = account.getExternalId();
        return (ids.getTeamIds().contains(externalId) && StringUtils.equals(account.getExternalIdType(), TEAM_SCOPE))
                || (ids.getOrgIds().contains(externalId) && StringUtils.equals(account.getExternalIdType(), ORG_SCOPE))
                || (StringUtils.equals(ids.getUserId(), externalId) && StringUtils.equals(account.getExternalIdType(), USER_SCOPE));
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest apiRequest) {
        String token = getTokenFromRequest(apiRequest);
        Map<String, Object> account = CollectionUtils.toMap(apiRequest.getRequestObject());

        String externalId = (String) account.get(EXTERNAL_ID);
        String externalIdType = (String) account.get(EXTERNAL_ID_TYPE);

        if (StringUtils.isNotEmpty(externalId) && StringUtils.isEmpty(externalIdType)) {
            Account currentAccount = (Account) obj;
            externalIdType = currentAccount.getExternalIdType();
        }
        externalId = getExternalId(externalId, externalIdType, token);
        if (StringUtils.isNotEmpty(externalId)) {
            account.put("externalId", externalId);
        }

        Account updatedAccount = (Account) getObjectManager().setFields(obj, account);
        getObjectProcessManager().scheduleStandardProcess(StandardProcess.UPDATE, obj, account);
        updatedAccount = getObjectManager().reload(updatedAccount);

        GithubUtils.ReverseMappings reverseMappings = githubUtils.validateAndFetchReverseMappings(token);
        Account modifiedAccount = transformProject(updatedAccount, reverseMappings);
        AccountPolicy policy = (AccountPolicy) ApiContext.getContext().getPolicy();
        policy.grantObjectAccess(modifiedAccount);

        return Arrays.asList(modifiedAccount);
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
