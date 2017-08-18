package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.iaas.api.auth.AchaiusPolicyOptionsFactory;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.iaas.api.auth.dao.AuthDao;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultAuthorizationProvider implements AuthorizationProvider {

    public static final String ACCOUNT_SCHEMA_FACTORY_NAME = "accountSchemaFactoryName";
    public static final String ACCOUNT_CLUSTER_ID = "accountClusterId";

    private static final Logger log = LoggerFactory.getLogger(DefaultAuthorizationProvider.class);

    Map<String, SchemaFactory> schemaFactories = new HashMap<>();
    AchaiusPolicyOptionsFactory optionsFactory;

    AuthDao authDao;
    AccountDao accountDao;

    public DefaultAuthorizationProvider(AchaiusPolicyOptionsFactory optionsFactory, AuthDao authDao, AccountDao accountDao,
            Map<String, SchemaFactory> schemaFactories) {
        super();
        this.optionsFactory = optionsFactory;
        this.authDao = authDao;
        this.accountDao = accountDao;
        this.schemaFactories = schemaFactories;
    }

    @Override
    public SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request) {
        Object name = request.getAttribute(ACCOUNT_SCHEMA_FACTORY_NAME);

        if (name == null) {
            name = getRole(policy, request);
        }

        if (name != null) {
            SchemaFactory schemaFactory = getByName(request, name.toString());
            if (schemaFactory == null) {
                log.error("Failed to find schema factory [{}]", name);
            } else {
                return schemaFactory;
            }
        }
        Account authenticatedAsAccount = accountDao.getAccountById(policy.getAuthenticatedAsAccountId());
        Policy authenticatedAsPolicy = getPolicy(authenticatedAsAccount, authenticatedAsAccount, policy.getIdentities(), request);
        return getByName(request, authDao.getRole(account, policy, authenticatedAsPolicy));
    }

    protected SchemaFactory getByName(ApiRequest request, String name) {
        String version = request.getSchemaVersion();
        if (version == null) {
            return schemaFactories.get(name);
        }

        SchemaFactory factory = schemaFactories.get(version + "-" + name);
        if (factory != null) {
            return factory;
        }

        return schemaFactories.get(name);
    }

    @Override
    public String getRole(Account account, Policy policy, ApiRequest request) {
        Object name = request.getAttribute(ACCOUNT_SCHEMA_FACTORY_NAME);

        if (name == null) {
            name = getRole(policy, request);
        }

        if (name != null) {
            SchemaFactory schemaFactory = schemaFactories.get(name.toString());
            if (schemaFactory == null) {
                log.error("Failed to find schema factory [{}]", name);
            } else {
                return name.toString();
            }
        }
        Account authenticatedAsAccount = accountDao.getAccountById(policy.getAuthenticatedAsAccountId());
        Policy authenticatedAsPolicy = getPolicy(authenticatedAsAccount, authenticatedAsAccount, policy.getIdentities(), request);
        String role = authDao.getRole(account, policy, authenticatedAsPolicy);
        SchemaFactory schemaFactory = schemaFactories.get(role);
        if (schemaFactory != null) {
            return role;
        }
        return null;
    }

    @Override
    public Policy getPolicy(Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        PolicyOptionsWrapper options = new PolicyOptionsWrapper(optionsFactory.getOptions(account));
        AccountPolicy policy = new AccountPolicy(account, authenticatedAsAccount, identities, options);

        String kind = getRole(policy, request);
        if (kind != null) {
            options = new PolicyOptionsWrapper(optionsFactory.getOptions(kind));
            options.setOption(Policy.ASSIGNED_ROLE, kind);
            policy = new AccountPolicy(account, authenticatedAsAccount, identities, options);
        }

        if (SubscriptionUtils.getSubscriptionStyle(policy) == SubscriptionStyle.QUALIFIED) {
            options.setOption(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER, FrameworkEvents.ACCOUNT_QUALIFIER);
            options.setOption(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER_VALUE, Long.toString(account.getId()));
        }

        Object clusterId = request.getAttribute(DefaultAuthorizationProvider.ACCOUNT_CLUSTER_ID);
        if (clusterId instanceof Long) {
            policy.setClusterId((Long) clusterId);
        }

        return policy;
    }

    protected String getRole(Policy policy, ApiRequest request) {
        String assignedRole = policy.getOption(Policy.ASSIGNED_ROLE);
        if (assignedRole != null) {
            return assignedRole;
        }
        if (policy.isOption(Policy.ROLE_OPTION)) {
            Object role = request.getOptions().get("_role");
            if (role != null && schemaFactories.containsKey(role)) {
                return role.toString();
            }
        }

        return null;
    }

    public static SubscriptionStyle getSubscriptionStyle(Account account, AchaiusPolicyOptionsFactory optionsFactory) {
        Policy tempPolicy = new AccountPolicy(account, account, null, optionsFactory.getOptions(account));
        return SubscriptionUtils.getSubscriptionStyle(tempPolicy);
    }

}
