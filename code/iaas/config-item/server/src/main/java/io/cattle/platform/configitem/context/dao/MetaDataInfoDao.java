package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.metadata.common.ContainerMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.ServiceMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface MetaDataInfoDao {
    public enum Version {
        version1("2015-07-25", "2015-07-25"),
        version2("2015-12-19", "2015-12-19"),
        latestVersion("latest", "2015-12-19");

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

    List<ContainerMetaData> getContainersData(long accountId);

    List<HostMetaData> getInstanceHostMetaData(long accountId, Instance instance);

    StackMetaData getStackMetaData(StackMetaData stackData, Version version);

    ServiceMetaData getServiceMetaData(ServiceMetaData serviceData, Version version);
}
