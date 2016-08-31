package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Stack;

public interface StackDao {
    Stack getStackByExternalId(Long accountId, String externalId);
}
