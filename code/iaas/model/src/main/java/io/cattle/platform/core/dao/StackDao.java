package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface StackDao {
    Stack getStackByExternalId(Long accountId, String externalId);

    Map<Long, List<Object>> getServicesForStack(List<Long> ids, IdFormatter idFormatter);
}
