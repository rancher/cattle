package io.cattle.platform.core.dao;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public interface LoadBalancerInfoDao {

    Map<Long, Pair<String, String>> getServiceIdToServiceStackName(long accountId);

    Map<Long, String> getInstanceIdToInstanceName(long accountId);

    Map<Long, String> getCertificateIdToCertificate(long accountId);

}
