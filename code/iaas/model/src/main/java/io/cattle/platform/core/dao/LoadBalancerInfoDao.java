package io.cattle.platform.core.dao;

import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.LBMetadataUtil.LBConfigMetadataStyle;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface LoadBalancerInfoDao {

    LBConfigMetadataStyle generateLBConfigMetadataStyle(Service lbService);

    // this method will be used for the v1->v2 upgrade
    LbConfig generateLBConfig(Service lbService);

    Map<Long, Pair<String, String>> getServiceIdToServiceStackName(long accountId);

    Map<Long, String> getInstanceIdToInstanceName(long accountId);

    Map<Long, String> getCertificateIdToCertificate(long accountId);

}
