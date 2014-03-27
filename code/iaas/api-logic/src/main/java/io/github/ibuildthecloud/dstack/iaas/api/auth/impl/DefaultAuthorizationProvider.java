package io.github.ibuildthecloud.dstack.iaas.api.auth.impl;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.pubsub.util.SubscriptionUtils;
import io.github.ibuildthecloud.dstack.api.pubsub.util.SubscriptionUtils.SubscriptionStyle;
import io.github.ibuildthecloud.dstack.core.model.Account;
import io.github.ibuildthecloud.dstack.iaas.api.auth.AchaiusPolicyOptionsFactory;
import io.github.ibuildthecloud.dstack.iaas.api.auth.AuthorizationProvider;
import io.github.ibuildthecloud.dstack.iaas.event.IaasEvents;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SubSchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class DefaultAuthorizationProvider implements AuthorizationProvider, InitializationTask, Priority {

    Map<String,SchemaFactory> schemaFactories = new HashMap<String, SchemaFactory>();
    List<SchemaFactory> schemaFactoryList;
    int priority = Priority.DEFAULT;
    AchaiusPolicyOptionsFactory optionsFactory;

    @Override
    public SchemaFactory getSchemaFactory(Account account, ApiRequest request) {
        return schemaFactories.get(account.getKind());
    }

    @Override
    public Policy getPolicy(Account account, ApiRequest request) {
        PolicyOptionsWrapper options = new PolicyOptionsWrapper(optionsFactory.getOptions(account));
        AccountPolicy policy = new AccountPolicy(account, options);

        if ( SubscriptionUtils.getSubscriptionStyle(policy) == SubscriptionStyle.QUALIFIED ) {
            options.setOption(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER, IaasEvents.ACCOUNT_QUALIFIER);
            options.setOption(SubscriptionUtils.POLICY_SUBSCRIPTION_QUALIFIER_VALUE, Long.toString(account.getId()));
        }

        return policy;
    }

    public static SubscriptionStyle getSubscriptionStyle(Account account, AchaiusPolicyOptionsFactory optionsFactory) {
        Policy tempPolicy = new AccountPolicy(account, optionsFactory.getOptions(account));
        return SubscriptionUtils.getSubscriptionStyle(tempPolicy);
    }


    public List<SchemaFactory> getSchemaFactoryList() {
        return schemaFactoryList;
    }

    @Inject
    public void setSchemaFactoryList(List<SchemaFactory> schemaFactoryList) {
        this.schemaFactoryList = schemaFactoryList;
    }

    @Override
    public void start() {
        for ( SchemaFactory factory : schemaFactoryList ) {
            if ( factory instanceof SubSchemaFactory ) {
                ((SubSchemaFactory)factory).init();
            }
            schemaFactories.put(factory.getId(), factory);
        }
    }

    @Override
    public void stop() {
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public AchaiusPolicyOptionsFactory getOptionsFactory() {
        return optionsFactory;
    }

    @Inject
    public void setOptionsFactory(AchaiusPolicyOptionsFactory optionsFactory) {
        this.optionsFactory = optionsFactory;
    }

}
