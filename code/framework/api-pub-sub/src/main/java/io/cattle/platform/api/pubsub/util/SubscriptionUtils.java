package io.cattle.platform.api.pubsub.util;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.auth.impl.PolicyOptions;

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
        return getSubscriptionStyleFromString(policy.getOption(POLICY_SUBSCRIPTION_STYLE));
    }

    public static SubscriptionStyle getSubscriptionStyle(PolicyOptions policyOptions) {
        return getSubscriptionStyleFromString(policyOptions.getOption(POLICY_SUBSCRIPTION_STYLE));
    }

    public static SubscriptionStyle getSubscriptionStyleFromString(String style) {
        if (style == null) {
            return SubscriptionStyle.QUALIFIED;
        }

        try {
            return SubscriptionStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SubscriptionStyle.QUALIFIED;
        }
    }

}
