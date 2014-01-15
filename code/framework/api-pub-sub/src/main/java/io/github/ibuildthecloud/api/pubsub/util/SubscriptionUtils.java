package io.github.ibuildthecloud.api.pubsub.util;

import io.github.ibuildthecloud.dstack.api.auth.Policy;

public class SubscriptionUtils {

    public static final String POLICY_SUBSCRIPTION_STYLE = "subscription.style";

    public enum SubscriptionStyle {
        RAW, ACCOUNT, AGENT
    }

    public static SubscriptionStyle getSubscriptionStyle(Policy policy) {
        String style = policy.getOption(POLICY_SUBSCRIPTION_STYLE);

        if ( style == null ) {
            return SubscriptionStyle.ACCOUNT;
        }

        try {
            return SubscriptionStyle.valueOf(style.toUpperCase());
        } catch ( IllegalArgumentException e ) {
            return SubscriptionStyle.ACCOUNT;
        }
    }

}
