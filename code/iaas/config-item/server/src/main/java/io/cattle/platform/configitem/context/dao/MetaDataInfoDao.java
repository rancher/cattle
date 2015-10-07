package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.ContainerMetaData;
import io.cattle.platform.configitem.context.data.HostMetaData;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface MetaDataInfoDao {

    List<ContainerMetaData> getContainersData(long accountId);

    List<? extends HostMetaData> getInstanceHostMetaData(long accountId, Instance instance);

}
