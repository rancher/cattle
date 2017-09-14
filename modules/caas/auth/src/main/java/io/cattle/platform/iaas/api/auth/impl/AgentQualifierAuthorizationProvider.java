package io.cattle.platform.iaas.api.auth.impl;

import io.cattle.platform.api.auth.Identity;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.auth.impl.OptionCallback;
import io.cattle.platform.api.auth.impl.PolicyOptions;
import io.cattle.platform.api.pubsub.model.Subscribe;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils;
import io.cattle.platform.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.iaas.api.auth.AchaiusPolicyOptionsFactory;
import io.cattle.platform.iaas.api.auth.AuthorizationProvider;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AgentQualifierAuthorizationProvider implements AuthorizationProvider {

    private static final Logger log = LoggerFactory.getLogger(AgentQualifierAuthorizationProvider.class);

    private static final Set<String> STATES = new HashSet<>(Arrays.asList(
            CommonStatesConstants.ACTIVATING,
            CommonStatesConstants.ACTIVE,
            AgentConstants.STATE_RECONNECTING,
            AgentConstants.STATE_DISCONNECTED,
            AgentConstants.STATE_DISCONNECTING,
            AgentConstants.STATE_RECONNECTED
    ));
    private static final Set<String> DISCONNECTED = new HashSet<>(Arrays.asList(
            AgentConstants.STATE_DISCONNECTED,
            AgentConstants.STATE_DISCONNECTING
    ));

    AchaiusPolicyOptionsFactory optionsFactory;
    ResourceManagerLocator locator;
    ObjectProcessManager processManager;

    public AgentQualifierAuthorizationProvider(AchaiusPolicyOptionsFactory optionsFactory, ResourceManagerLocator locator,
            ObjectProcessManager processManager) {
        this.optionsFactory = optionsFactory;
        this.locator = locator;
        this.processManager = processManager;
    }

    @Override
    public Policy getPolicy(final Account account, Account authenticatedAsAccount, Set<Identity> identities, ApiRequest request) {
        PolicyOptions policyOptions = optionsFactory.getOptions(account);

        boolean apply = false;
        final SubscriptionStyle accountStyle = SubscriptionUtils.getSubscriptionStyle(policyOptions);

        /* This boolean logic could be optimized but this seems more readable. */
        if (accountStyle == SubscriptionStyle.RAW) {
            apply = true;
        } else if (accountStyle == SubscriptionStyle.QUALIFIED && AccountConstants.AGENT_KIND.equals(account.getKind())) {
            apply = true;
        }

        if (!apply) {
            return null;
        }

        final PolicyOptionsWrapper options = new PolicyOptionsWrapper(policyOptions);
        AccountPolicy policy = new AccountPolicy(account, authenticatedAsAccount, identities, options);

        options.addCallback(Policy.AGENT_ID, new OptionCallback() {
            @Override
            public String getOption() {
                Long agentId = getAgent();
                return agentId == null ? null : Long.toString(agentId);
            }
        });

        options.addCallback(SubscriptionUtils.POLICY_SUBSCRIPTION_STYLE, new OptionCallback() {
            @Override
            public String getOption() {
                if (accountStyle == SubscriptionStyle.RAW && getRawAgentId() != null) {
                    return SubscriptionStyle.QUALIFIED.toString();
                }

                return accountStyle.toString();
            }
        });

        options.setOption(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER, FrameworkEvents.AGENT_QUALIFIER);
        options.addCallback(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER_VALUE, new OptionCallback() {
            @Override
            public String getOption() {
                String agentId = options.getOption(Policy.AGENT_ID);

                if (agentId == null) {
                    log.error("Failed to determine the proper agent ID for subscription for account [{}]", account.getId());
                    throw new ClientVisibleException(ResponseCodes.FORBIDDEN);
                }

                return agentId;
            }
        });

        return policy;
    }

    protected Long getRawAgentId() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        Subscribe subscribe = request.proxyRequestObject(Subscribe.class);
        try {
            Long agentId = subscribe.getAgentId();
            if (agentId != null) {
                return agentId;
            }
        } catch (Throwable t) {
            // ignore errors;
        }

        return null;
    }

    protected Long getAgent() {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        Long agentId = getRawAgentId();
        if (agentId != null) {
            return agentId;
        }

        String type = request.getSchemaFactory().getSchemaName(Agent.class);
        ResourceManager rm = locator.getResourceManagerByType(type);
        Long id = null;

        /*  This really isn't the best logic.  Basically we are looking for agents with state in STATES */
        for (Object obj : rm.list(type, null, Pagination.limit(2))) {
            if (!(obj instanceof Agent)) {
                continue;
            }

            Agent agent = (Agent)obj;

            if (STATES.contains(agent.getState())) {
                if (id != null) {
                    throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "agentId");
                } else {
                    if (DISCONNECTED.contains(agent.getState())) {
                        processManager.scheduleProcessInstance(AgentConstants.PROCESS_RECONNECT, agent, null);
                    }
                    id = agent.getId();
                }
            }
        }

        return id;
    }

    @Override
    public SchemaFactory getSchemaFactory(Account account, Policy policy, ApiRequest request) {
        return null;
    }

    @Override
    public String getRole(Account account, Policy policy, ApiRequest request) {
        return null;
    }

}
