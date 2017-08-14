package io.cattle.platform.api.resource.jooq;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Map;

public class JooqAccountAuthorization {

    ObjectMetaDataManager metaDataManager;

    public JooqAccountAuthorization(ObjectMetaDataManager metaDataManager) {
        super();
        this.metaDataManager = metaDataManager;
    }

    public void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
        if (policy.isOption(Policy.LIST_ALL_ACCOUNTS)) {
            return;
        }

        if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && (byId || byLink)) {
            return;
        }

        if ("account".equals(type)) {
            criteria.put(ObjectMetaDataManager.ID_FIELD, policy.getAccountId());
            return;
        }

        criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, policy.getAccountId());
        criteria.put(ObjectMetaDataManager.CLUSTER_FIELD, policy.getClusterId());
    }

}
