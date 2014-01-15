package io.github.ibuildthecloud.dstack.core.dao;

import io.github.ibuildthecloud.dstack.core.model.InstanceHostMap;

import java.util.List;

public interface InstanceDao {

    List<? extends InstanceHostMap> findNonRemovedInstanceHostMaps(long instanceId);

}
