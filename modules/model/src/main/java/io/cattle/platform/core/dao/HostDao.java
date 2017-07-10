package io.cattle.platform.core.dao;

import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.List;
import java.util.Map;

public interface HostDao {

    DynamicLongProperty HOST_REMOVE_DELAY = ArchaiusUtil.getLong("host.remove.delay.seconds");
    DynamicLongProperty HOST_REMOVE_START_DELAY = ArchaiusUtil.getLong("host.remove.delay.startup.seconds");

    boolean hasActiveHosts(Long accountId);

    Map<Long, List<Object>> getInstancesPerHost(List<Long> hosts, IdFormatter idFormatter);

    List<? extends Host> findHostsRemove();

}
