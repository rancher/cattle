package io.cattle.platform.iaas.api.auth.integration.internal.rancher;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.AgentTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ProjectConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.cattle.platform.iaas.api.auth.integration.interfaces.AccountLookup;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class BasicAuthImpl implements AccountLookup, Priority {

    public static final String AUTH_HEADER = "Authorization";
    public static final String CHALLENGE_HEADER = "WWW-Authenticate";
    public static final String BASIC = "Basic";
    public static final String BASIC_REALM = "Basic realm=\"%s\"";
    private static final String NO_CHALLENGE_HEADER = "X-API-No-Challenge";
    private static final String CONNECTION = "Connection";

    private static final DynamicStringProperty REALM = ArchaiusUtil.getString("api.auth.realm");

    AuthDao authDao;
    @Inject
    AdminAuthLookUp adminAuthLookUp;
    @Inject
    ObjectManager objectManager;
    @Inject
    TokenAuthLookup tokenAuthLookUp;

    @Override
    public Account getAccount(ApiRequest request) {
        String[] auth = getUsernamePassword(request.getServletContext().getRequest().getHeader(AUTH_HEADER));
        if (auth == null) {
            return null;
        }
        Account account = authDao.getAccountByKeys(auth[0], auth[1], ApiContext.getContext().getTransformationService());
        if (account != null) {
            return switchAccount(account, request);
        } else if (auth[0].toLowerCase().startsWith(ProjectConstants.OAUTH_BASIC.toLowerCase()) && SecurityConstants.SECURITY.get()) {
            String[] splits = auth[0].split("=");
            String projectId = splits.length == 2 ? splits[1] : null;
            request.setAttribute(ProjectConstants.PROJECT_HEADER, projectId);
            account = tokenAuthLookUp.getAccountAccess(ProjectConstants.AUTH_TYPE + auth[1], request);
        } else if (auth[0].toLowerCase().startsWith(ProjectConstants.OAUTH_BASIC.toLowerCase()) && !SecurityConstants.SECURITY.get()) {
            String[] splits = auth[0].split("=");
            String projectId = splits.length == 2 ? splits[1] : null;
            request.setAttribute(ProjectConstants.PROJECT_HEADER, projectId);
            account = adminAuthLookUp.getAccount(request);
        }
        return account;
    }

    protected Account switchAccount(Account account, ApiRequest request) {
        boolean shouldSwitch = DataAccessor.fromDataFieldOf(account)
            .withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ACCOUNT)
            .withDefault(false)
                .as(Boolean.class);

        boolean projectAdmin = false;
        if (DataAccessor.fromDataFieldOf(account)
                .withKey(AccountConstants.DATA_ACT_AS_RESOURCE_ADMIN_ACCOUNT)
                .withDefault(false)
                .as(Boolean.class)) {
            shouldSwitch = true;
            projectAdmin = true;
        }

        if (shouldSwitch) {
            Long agentOwnerId = DataAccessor.fromDataFieldOf(account).withKey(AccountConstants.DATA_AGENT_OWNER_ID).as(Long.class);
            Agent agent = null;
            if (agentOwnerId != null) {
                agent = objectManager.findAny(Agent.class, AGENT.ID, agentOwnerId);
            } else {
                agent = objectManager.findAny(Agent.class, AGENT.ACCOUNT_ID, account.getId()); 
            }

            if (agent != null) {
                Long resourceAccId = DataAccessor.fromDataFieldOf(agent)
                        .withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID)
                        .as(Long.class);
                if (resourceAccId != null) {
                    List<Object> activeStates = new ArrayList<>();
                    activeStates.add(CommonStatesConstants.ACTIVE);
                    activeStates.add(ServiceConstants.STATE_UPGRADING);
                    Account resourceAccount = objectManager.findAny(Account.class,
                            ACCOUNT.ID, resourceAccId,
                            ACCOUNT.STATE, new Condition(ConditionType.IN, activeStates));
                    if (resourceAccount == null) {
                        return null;
                    }
                    if (projectAdmin) {
                        resourceAccount.setKind("projectadmin");
                    } else {
                        resourceAccount.setKind("environment");
                    }
                    return resourceAccount;
                }
            }
        }

        return account;
    }

    @Override
    public boolean challenge(ApiRequest request) {
        if ("upgrade".equalsIgnoreCase(request.getServletContext().getRequest().getHeader(CONNECTION))) {
            return false;
        }
        if ("true".equalsIgnoreCase(request.getServletContext().getRequest().getHeader(NO_CHALLENGE_HEADER))) {
            return false;
        }
        HttpServletResponse response = request.getServletContext().getResponse();
        String realm = REALM.get();

        if (realm == null) {
            response.setHeader(CHALLENGE_HEADER, BASIC);
        } else {
            response.setHeader(CHALLENGE_HEADER, String.format(BASIC_REALM, realm));
        }

        return true;
    }

    protected String getRealm(ApiRequest request) {
        return REALM.get();
    }

    public static String[] getUsernamePassword(ApiRequest request) {
        return getUsernamePassword(request.getServletContext().getRequest().getHeader(AUTH_HEADER));
    }

    public static String[] getUsernamePassword(String auth) {
        if (auth == null)
            return null;

        String[] parts = StringUtils.split(auth);

        if (parts.length != 2) {
            return null;
        }

        if (!parts[0].equalsIgnoreCase(BASIC))
            return null;

        try {
            String text = new String(Base64.decodeBase64(parts[1]), "UTF-8");
            int i = text.indexOf(":");
            if (i == -1) {
                return null;
            }

            return new String[]{text.substring(0, i), text.substring(i + 1)};
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    public AuthDao getAuthDao() {
        return authDao;
    }

    @Inject
    public void setAuthDao(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String getName() {
        return "BasicAuth";
    }
}
