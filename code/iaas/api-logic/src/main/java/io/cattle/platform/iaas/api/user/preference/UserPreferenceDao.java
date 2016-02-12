package io.cattle.platform.iaas.api.user.preference;

import io.cattle.platform.core.model.UserPreference;

public interface UserPreferenceDao {
    boolean isUnique(UserPreference userPreference, long accountId);
}
