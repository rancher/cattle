package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Vnet;

public interface VnetDao {

    Vnet findVnetFromHosts(Long instanceId, Long subnetId);

}
