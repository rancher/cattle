package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.ContainerMetaData;

import java.util.List;

public interface MetaDataInfoDao {

    List<ContainerMetaData> getServicesContainersData(long accountId);

    List<ContainerMetaData> getStandaloneContainersData(long accountId);

}
