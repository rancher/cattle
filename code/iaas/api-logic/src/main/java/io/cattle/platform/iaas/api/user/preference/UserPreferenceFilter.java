package io.cattle.platform.iaas.api.user.preference;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.model.UserPreference;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserPreferenceFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    UserPreferenceDao userPreferenceDao;

    private static final Logger log = LoggerFactory.getLogger(UserPreferenceFilter.class);

    private static final String ALL = "all";
    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { UserPreference.class };
    }

    @Override
    public Object list(String type, ApiRequest request, ResourceManager next) {
        if (!request.getConditions().containsKey(ALL)) {
            addAccountIdCondition(request);
        } else {
            if (!request.getConditions().get(ALL).isEmpty()) {
                for (Condition condition:request.getConditions().get(ALL)) {
                    if (condition.getConditionType().equals(ConditionType.EQ)
                            && !Boolean.parseBoolean(String.valueOf(condition.getValue()))) {
                        addAccountIdCondition(request);
                    } else {
                        log.debug("Dont add account id condition.");
                    }
                }
            }
        }
        return super.list(type, request, next);
    }

    private void addAccountIdCondition(ApiRequest request) {
        Map<String, List<Condition>> conditions = request
                .getConditions();
        if (conditions.get(ObjectMetaDataManager.ACCOUNT_FIELD) == null || conditions.get(ObjectMetaDataManager.ACCOUNT_FIELD).isEmpty()) {
            if (conditions.get(ObjectMetaDataManager.ACCOUNT_FIELD) == null) {
                conditions.put(ObjectMetaDataManager.ACCOUNT_FIELD, new ArrayList<Condition>());
            }
            try {
                long accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
                conditions.get(ObjectMetaDataManager.ACCOUNT_FIELD).add(new Condition(ConditionType.EQ, accountId));
            } catch (NullPointerException e) {
                log.debug("Unable to get account id.");
            }
        }
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        UserPreference userPreference = request.proxyRequestObject(UserPreference.class);
        long accountId;
        try {
            accountId = ((Policy) ApiContext.getContext().getPolicy()).getAccountId();
            if (!userPreferenceDao.isUnique(userPreference, accountId)) {
                throw new ValidationErrorException(ValidationErrorCodes.NOT_UNIQUE, CredentialConstants.PUBLIC_VALUE);
            }
            return super.create(type, request, next);
        } catch (NullPointerException e) {
            return super.create(type, request, next);
        }

    }
}
