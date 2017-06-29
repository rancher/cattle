package io.cattle.platform.api.resource.jooq;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

import org.jooq.Condition;
import org.jooq.TableField;

public class JooqAccountAuthorization {

    ObjectMetaDataManager metaDataManager;

    public JooqAccountAuthorization(ObjectMetaDataManager metaDataManager) {
        super();
        this.metaDataManager = metaDataManager;
    }

    private void addBaseAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
        if (!policy.isOption(Policy.LIST_ALL_ACCOUNTS)) {
            if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && (byId || byLink)) {
                return;
            }

            if ("account".equals(type)) {
                criteria.put(ObjectMetaDataManager.ID_FIELD, policy.getAccountId());
            } else {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, policy.getAccountId());
            }
        }
    }

    public void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
        addBaseAccountAuthorization(byId, byLink, type, criteria, policy);

        if (!policy.isOption(Policy.LIST_ALL_ACCOUNTS)) {
            if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && (byId || byLink)) {
                return;
            }

            TableField<?, Object> accountField = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.ACCOUNT_FIELD);
            TableField<?, Object> publicField = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.PUBLIC_FIELD);
            Object accountValue = criteria.get(ObjectMetaDataManager.ACCOUNT_FIELD);

            if (accountField == null || publicField == null || accountValue == null) {
                return;
            }

            ApiRequest request = ApiContext.getContext().getApiRequest();
            // Only allow is_public logic for GET methods
            if (request == null) {
                return;
            }

            if ("GET".equals(request.getMethod()) || ("POST".equals(request.getMethod()) && request.getAction() == null)) {
                criteria.remove(ObjectMetaDataManager.ACCOUNT_FIELD);
                Condition accountCondition = null;
                if (accountValue instanceof io.github.ibuildthecloud.gdapi.condition.Condition) {
                    accountCondition = accountField.in(((io.github.ibuildthecloud.gdapi.condition.Condition) accountValue).getValues());
                } else {
                    accountCondition = accountField.eq(accountValue);
                }

                criteria.put(Condition.class, publicField.isTrue().or(accountCondition));
            }

        }
    }


}
