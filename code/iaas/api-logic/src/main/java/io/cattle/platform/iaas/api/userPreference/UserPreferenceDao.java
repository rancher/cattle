package io.cattle.platform.iaas.api.userPreference;

import io.cattle.platform.core.model.UserPreference;

public interface UserPreferenceDao {
    boolean isUnique(UserPreference userPreference, long accountId);
}
