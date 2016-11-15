package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.configitem.context.data.metadata.common.NetworkMetaData;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MetaDataInfoDao {

    public enum Version {
        version1("2015-07-25", "2015-07-25"),
        version2("2015-12-19", "2015-12-19"),
        version3("2016-07-29", "2016-07-29"),
        latestVersion("latest", "2016-07-29");

        String tag;
        String value;

        private Version(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }

        public String getTag() {
            return this.tag;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }
    }

    // get containers data
    List<ContainerMetaData> getManagedContainersData(MetaHelperInfo helperInfo,
            Map<Long, Map<String, String>> containerIdToContainerLink);

    List<ContainerMetaData> getNetworkFromContainersData(Map<Long, String> instanceIdToUUID, MetaHelperInfo helperInfo);

    List<ContainerMetaData> getHostContainersData(MetaHelperInfo helperInfo);

    // helper data
    List<String> getPrimaryIpsOnInstanceHost(long hostId);

    Map<Long, List<HealthcheckInstanceHostMap>> getInstanceIdToHealthCheckers(Account account);

    Map<Long, HostMetaData> getHostIdToHostMetadata(Account account, Map<Long, Account> accounts,
            Set<Long> linkedServicesIds);

    List<NetworkMetaData> getNetworksMetaData(MetaHelperInfo helperInfo);

    Map<Long, Map<String, String>> getContainerIdToContainerLink(long accountId);
}
