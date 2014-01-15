package io.github.ibuildthecloud.api.pubsub.util;

import io.github.ibuildthecloud.dstack.api.auth.Policy;

public class SubscriptionUtils {

    public static final String POLICY_SUBSCRIPTION_STYLE = "subscription.style";
    public static final String POLICY_SUBSCRIPTION_QUALIFIER = "subscription.qualifier";
    public static final String POLICY_SUBSCRIPTION_QUALIFIER_VALUE = "subscription.qualifier.value";

    public enum SubscriptionStyle {
        RAW, QUALIFIED
    }

    public static String getSubscriptionQualifier(Policy policy) {
        return policy.getOption(POLICY_SUBSCRIPTION_QUALIFIER);
    }

    public static String getSubscriptionQualifierValue(Policy policy) {
        String value = policy.getOption(POLICY_SUBSCRIPTION_QUALIFIER_VALUE);
        return value == null ? Long.toString(policy.getAccountId()) : value;
    }

    public static SubscriptionStyle getSubscriptionStyle(Policy policy) {
        String style = policy.getOption(POLICY_SUBSCRIPTION_STYLE);

        if ( style == null ) {
            return SubscriptionStyle.QUALIFIED;
        }

        try {
            return SubscriptionStyle.valueOf(style.toUpperCase());
        } catch ( IllegalArgumentException e ) {
            return SubscriptionStyle.QUALIFIED;
        }
    }

}
