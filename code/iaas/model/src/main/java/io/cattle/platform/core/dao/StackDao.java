package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.ScheduledUpgrade;
import io.cattle.platform.core.model.Stack;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface StackDao {
    Stack getStackByExternalId(Long accountId, String externalId);

    Map<Long, List<Object>> getServicesForStack(List<Long> ids, IdFormatter idFormatter);

    List<? extends Stack> getStacksToUpgrade(Collection<String> currentIds);

    List<? extends ScheduledUpgrade> getRunningUpgrades();

    List<? extends ScheduledUpgrade> getReadyUpgrades(Set<Long> accountsToIgnore, int max);

    boolean hasSkipServices(long stackId);

}
