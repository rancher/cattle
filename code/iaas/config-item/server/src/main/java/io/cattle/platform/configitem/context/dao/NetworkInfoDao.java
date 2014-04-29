package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;

import java.util.List;
import java.util.Map;

import com.netflix.config.DynamicStringProperty;

public interface NetworkInfoDao {

    public static final DynamicStringProperty DEFAULT_DOMAIN = ArchaiusUtil.getString("default.network.domain");

    List<?> networkClients(Instance instance);

    List<? extends NetworkService> networkServices(Instance instance);

    Map<Long,Network> networks(Instance instance);

}
