package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.ContainerMetaData;

import java.util.List;

public interface MetaDataInfoDao {

    List<ContainerMetaData> getContainersData(long accountId);

}
