package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.UserPreference;

public interface UserPreferenceDao {

    boolean isUnique(UserPreference userPreference, long accountId);

}
